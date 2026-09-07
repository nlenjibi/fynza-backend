package ecommerce.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import ecommerce.common.config.JwtCacheConfig.CachedAuthentication;
import ecommerce.common.security.JwtTokenProvider;
import ecommerce.common.security.TokenBlacklistService;
import ecommerce.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single-responsibility service that owns the full JWT validation pipeline.
 *
 * <p>Checks are ordered cheapest-first so that the expensive DB call
 * ({@code loadUserById}) is skipped whenever a cheaper check fails:
 *
 * <ol>
 *   <li>JWT signature / expiry (CPU-only)</li>
 *   <li>Token blacklist  (in-memory Caffeine + Bloom Filter)</li>
 *   <li>Token version    (in-memory Caffeine)</li>
 *   <li>Load UserPrincipal (<b>DB – cached</b> via Spring Cache)</li>
 *   <li>Account-lock flag (from cached principal)</li>
 *   <li>Password-change timestamp (from cached principal)</li>
 * </ol>
 *
 * <p>Returns {@code null} when the token is absent or all checks pass
 * (the filter continues the chain), or an {@link AuthenticationException}
 * subclass that {@link CustomAuthenticationEntryPoint} knows how to render.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final Cache<String, CachedAuthentication> tokenValidationCache;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Validates {@code jwt} through the full security pipeline (synchronous).
     * Uses token-level caching to avoid repeated JWT parsing and principal loading.
     *
     * @return {@code null}  – no token present OR all checks passed
     *         (caller should continue the filter chain and, when non-null
     *         Authentication is also returned, set it in the SecurityContext)
     * @return non-null {@link AuthenticationException} – a check failed;
     *         caller should reject the request via the entry point
     */
    public ValidationResult validate(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return ValidationResult.noToken();
        }

        // ── 1. Signature / expiry (CPU, no I/O) ───────────────────────────────
        if (!jwtTokenProvider.validateToken(jwt)) {
            return ValidationResult.invalid();
        }

        // ── 2. Blacklist check (unconditional) ────────────────────────────────
        if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
            log.warn("Blacklisted token attempted");
            return ValidationResult.reject(
                    new BadCredentialsException("Token has been revoked. Please login again."));
        }

        // ── 3. Token-version check (unconditional) ────────────────────────────
        UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
        long issuedAt = jwtTokenProvider.getIssuedAtMillis(jwt);
        if (!tokenBlacklistService.isUserTokenVersionValid(userId, issuedAt)) {
            log.warn("Token version invalid for user: {}", userId);
            return ValidationResult.reject(
                    new InsufficientAuthenticationException(
                            "Session has been invalidated. Please login again."));
        }

        // ── 4. Load principal — @Cacheable("userPrincipals") handles DB caching;
        //       evicted on password change, lock/unlock, role update via evictPrincipal()
        UserPrincipal principal = loadPrincipal(userId);

        // ── 5. Account-lock check ─────────────────────────────────────────────
        if (principal.isAccountLocked()) {
            log.warn("Locked account attempted: {}", userId);
            return ValidationResult.reject(
                    new LockedException("Account is locked. Please contact support."));
        }

        // ── All checks passed ─────────────────────────────────────────────────
        log.debug("JWT validated for user {}", userId);
        return ValidationResult.valid(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Loads a {@link UserPrincipal} by id. Results are cached under the key
     * {@code userId} in the {@code "userPrincipals"} cache so that repeated
     * requests within the cache TTL skip the DB entirely.
     *
     * <p>Evict the entry (via {@link #evictPrincipal}) whenever the user's
     * password, lock state, or role changes.
     *
     * @param userId The user's unique identifier (UUID)
     */
    @Cacheable(value = "userPrincipals", key = "#userId.toString()")
    public UserPrincipal loadPrincipal(UUID userId) {
        return (UserPrincipal) customUserDetailsService.loadUserById(userId);
    }


    /**
     * Evicts the cached {@link UserPrincipal} for {@code userId}.
     * Call this after any operation that changes the user's security-relevant
     * state: password change, lock/unlock, role update, logout-all.
     *
     * @param userId The user's unique identifier (UUID)
     */
    @CacheEvict(value = "userPrincipals", key = "#userId.toString()")
    public void evictPrincipal(UUID userId) {
        log.debug("Evicted cached UserPrincipal for user {}", userId);
    }


    /**
     * Evicts a specific token from the validation cache.
     * Call this on logout to immediately invalidate the token cache.
     */
    public void evictToken(String token) {
        if (token != null) {
            tokenValidationCache.invalidate(token);
            log.debug("Evicted token from validation cache");
        }
    }


    // ── Result type ────────────────────────────────────────────────────────────

    /**
     * Immutable result returned by {@link #validate}.
     *
     * <ul>
     *   <li>{@link Status#NO_TOKEN}  – no Authorization header; continue chain unauthenticated</li>
     *   <li>{@link Status#INVALID}   – bad / expired JWT; continue chain unauthenticated</li>
     *   <li>{@link Status#REJECT}    – security check failed; abort with 401</li>
     *   <li>{@link Status#VALID}     – all checks passed; set authentication and continue</li>
     * </ul>
     */
    public record ValidationResult(
            Status status,
            Authentication authentication,
            AuthenticationException error) {

        public enum Status { NO_TOKEN, INVALID, REJECT, VALID }

        static ValidationResult noToken()  { return new ValidationResult(Status.NO_TOKEN,  null, null); }
        static ValidationResult invalid()  { return new ValidationResult(Status.INVALID,   null, null); }
        static ValidationResult reject(AuthenticationException ex) {
            return new ValidationResult(Status.REJECT, null, ex);
        }
        static ValidationResult valid(Authentication auth) {
            return new ValidationResult(Status.VALID, auth, null);
        }

        public boolean shouldReject()    { return status == Status.REJECT; }
        public boolean shouldAuthenticate() { return status == Status.VALID; }
    }
}

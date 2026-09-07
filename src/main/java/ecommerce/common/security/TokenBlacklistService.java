package ecommerce.common.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT token blacklist and user token versioning.
 * 
 * <p>Uses SHA-256 hashing for blacklisted tokens to ensure secure storage.
 * Tracks token versions per user to enable immediate invalidation of all
 * user tokens (e.g., on password change or logout from all devices).
 * 
 * <p>Performance optimizations:
 * - Bloom Filter for O(1) quick rejection of valid tokens
 * - Caffeine cache with soft values for memory efficiency under pressure
 * - Synchronized Bloom Filter updates when tokens are blacklisted
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final com.github.benmanes.caffeine.cache.Cache<String, Boolean> tokenBlacklist;
    private final com.github.benmanes.caffeine.cache.Cache<String, Long> userTokenVersion;
    private final BloomFilterService bloomFilterService;

    public TokenBlacklistService(
            @Value("${jwt.blacklist.max-size:10000}") int maxSize,
            @Value("${jwt.blacklist.expire-after-write-hours:24}") int expireAfterWriteHours,
            BloomFilterService bloomFilterService) {

        this.bloomFilterService = bloomFilterService;

        // Caffeine with soft values to allow GC under memory pressure
        this.tokenBlacklist = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteHours, TimeUnit.HOURS)
                .recordStats()
                .build();

        this.userTokenVersion = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(expireAfterWriteHours, TimeUnit.HOURS)
                .softValues()
                .build();

        log.info("Token blacklist initialized with maxSize={}, expireAfterWriteHours={}, softValues=true",
                maxSize, expireAfterWriteHours);
    }

    /**
     * Add a token to the blacklist.
     * @param token The JWT token to blacklist
     * @param expirationTime Token expiration timestamp in milliseconds
     */
    public void blacklistToken(String token, long expirationTime) {
        String tokenKey = hashToken(token);
        tokenBlacklist.put(tokenKey, true);

        // Synchronize with Bloom Filter for O(1) quick rejection
        bloomFilterService.add(tokenKey);

        long remainingTime = Math.max(expirationTime - System.currentTimeMillis(), 0);
        log.debug("Token blacklisted, remaining time: {}ms", remainingTime);
    }

    /**
     * Check if a token is blacklisted.
     * Uses Bloom Filter for quick rejection, then verifies with Caffeine.
     * 
     * @param token The JWT token to check
     * @return true if blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        String tokenKey = hashToken(token);
        
        // First, check Bloom Filter for quick rejection (O(1))
        // If Bloom Filter says NO, token is definitely not blacklisted
        if (!bloomFilterService.mightContain(tokenKey)) {
            return false;
        }
        
        // Bloom Filter said MAYBE - verify with Caffeine cache
        // This is required to avoid false positives
        boolean isBlacklisted = tokenBlacklist.getIfPresent(tokenKey) != null;
        if (isBlacklisted) {
            log.debug("Blacklisted token detected (verified by cache)");
        }
        return isBlacklisted;
    }

    /**
     * Invalidate all tokens for a specific user by updating their token version.
     * Uses UUID for user identification.
     * @param userId The user's unique identifier (UUID)
     */
    public void invalidateUserTokens(UUID userId) {
        String userKey = "user_" + userId.toString();
        long newVersion = System.currentTimeMillis();
        userTokenVersion.put(userKey, newVersion);
        log.info("Invalidated all tokens for user: {}", userId);
    }


    /**
     * Check if a token (identified by its issuedAt timestamp) is still valid
     * for the given user. Returns false if {@link #invalidateUserTokens} was
     * called AFTER the token was issued.
     *
     * @param userId         the user's unique identifier
     * @param tokenIssuedAt  the token's {@code iat} claim value in milliseconds
     * @return true if the token pre-dates any stored invalidation, false otherwise
     */
    public boolean isUserTokenVersionValid(UUID userId, long tokenIssuedAt) {
        Long invalidationTimestamp = userTokenVersion.getIfPresent("user_" + userId);
        return invalidationTimestamp == null || tokenIssuedAt >= invalidationTimestamp;
    }

    /**
     * Clear expired entries from the blacklist.
     */
    public void clearExpiredTokens() {
        tokenBlacklist.cleanUp();
        log.debug("Expired tokens cleared from blacklist");
    }

    /**
     * Get statistics about the token blacklist.
     * @return TokenBlacklistStats with current statistics
     */
    public TokenBlacklistStats getStats() {
        return new TokenBlacklistStats(
                tokenBlacklist.estimatedSize(),
                tokenBlacklist.stats().hitRate(),
                tokenBlacklist.stats().missRate()
        );
    }

    /**
     * Hash a token using SHA-256 for secure storage.
     * @param token The token to hash
     * @return The hashed token
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available, falling back to hashCode", e);
            return String.valueOf(token.hashCode());
        }
    }

    /**
     * Record containing token blacklist statistics.
     */
    public record TokenBlacklistStats(
            long currentSize,
            double hitRate,
            double missRate
    ) {}
}

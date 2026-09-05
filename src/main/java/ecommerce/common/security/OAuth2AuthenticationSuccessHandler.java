package ecommerce.common.security;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.entity.LinkedIdentity;
import ecommerce.modules.auth.repository.LinkedIdentityRepository;
import ecommerce.modules.auth.service.AuthService;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Handles successful OAuth2 logins (Google, GitHub, Facebook).
 *
 * <p>Flow:
 * <ol>
 *   <li>Extract user attributes from the OAuth2 provider's token</li>
 *   <li>Upsert the {@link User} row (email is the stable identity key)</li>
 *   <li>Upsert a {@link LinkedIdentity} row for this provider + providerUserId pair</li>
 *   <li>Mint JWT access + refresh tokens via {@link AuthService#oauth2Login}</li>
 *   <li>Set {@code HttpOnly} cookies and redirect to the frontend</li>
 * </ol>
 *
 * <h3>Cross-origin cookie behaviour</h3>
 * SameSite=None + Secure=true is required when the API and frontend run on different
 * origins. Set {@code app.cookie.secure=false} for local HTTP dev (Lax is used instead).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.success-redirect:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.jwt.access-token-expiry-minutes:15}")
    private int accessTokenExpiryMinutes;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final LinkedIdentityRepository linkedIdentityRepository;
    private final PasswordEncoder passwordEncoder;

    // Providers where email ownership is implicit (no email_verified claim needed)
    private static final java.util.Set<String> VERIFIED_BY_DEFAULT =
            java.util.Set.of("github", "facebook");

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String provider = "unknown";
            if (authentication instanceof OAuth2AuthenticationToken token) {
                provider = token.getAuthorizedClientRegistrationId();
            }

            log.info("OAuth2 login via provider='{}'", provider);

            String email    = extractEmail(oAuth2User, provider);
            String name     = extractName(oAuth2User);
            String avatar   = extractAvatar(oAuth2User, provider);
            String providerId = extractProviderId(oAuth2User, provider);

            if (email == null) {
                throw new IllegalStateException("Email not provided by OAuth2 provider: " + provider);
            }

            // Google returns email_verified; GitHub/Facebook verify emails at account creation
            if (!VERIFIED_BY_DEFAULT.contains(provider)) {
                Boolean verified = oAuth2User.getAttribute("email_verified");
                if (!Boolean.TRUE.equals(verified)) {
                    throw new IllegalStateException("Email not verified by provider: " + provider);
                }
            }

            // ── Upsert User ───────────────────────────────────────────────────────
            final String finalProvider = provider;
            User user = userRepository.findByEmail(email)
                    .map(existing -> {
                        patchExistingUser(existing, name, avatar);
                        return userRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        User created = buildNewOAuthUser(finalProvider, email, name, avatar);
                        User saved = userRepository.save(created);
                        log.info("Created new OAuth2 user email='{}' provider='{}'", email, finalProvider);
                        return saved;
                    });

            // ── Upsert LinkedIdentity ─────────────────────────────────────────────
            upsertLinkedIdentity(user.getId(), provider, providerId, email, name, avatar);

            // ── Mint tokens + set cookies ─────────────────────────────────────────
            AuthResponse authResponse = authService.oauth2Login(user, request);

            String sameSite = secureCookie ? "None" : "Lax";

            ResponseCookie accessCookie = ResponseCookie.from("access_token", authResponse.getAccessToken())
                    .httpOnly(true).secure(secureCookie).path("/")
                    .maxAge(Duration.ofMinutes(accessTokenExpiryMinutes)).sameSite(sameSite).build();

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                    .httpOnly(true).secure(secureCookie).path("/")
                    .maxAge(Duration.ofDays(refreshTokenExpiryDays)).sameSite(sameSite).build();

            response.addHeader("Set-Cookie", accessCookie.toString());
            response.addHeader("Set-Cookie", refreshCookie.toString());
            response.sendRedirect(frontendUrl);

        } catch (Exception e) {
            log.error("OAuth2 success handler failed", e);
            response.sendRedirect(frontendUrl + "/auth/login?error=oauth2_failure");
        }
    }

    // ── Attribute extraction ──────────────────────────────────────────────────

    private String extractEmail(OAuth2User user, String provider) {
        return user.getAttribute("email");
    }

    private String extractName(OAuth2User user) {
        String name = user.getAttribute("name");
        if (name == null) {
            String login = user.getAttribute("login"); // GitHub uses 'login'
            if (login != null) return login;
        }
        return name;
    }

    private String extractAvatar(OAuth2User user, String provider) {
        String picture = user.getAttribute("picture");   // Google
        if (picture == null) picture = user.getAttribute("avatar_url"); // GitHub
        return picture;
    }

    private String extractProviderId(OAuth2User user, String provider) {
        // Google: "sub"  |  GitHub & Facebook: "id" (may come as Integer)
        Object id = switch (provider) {
            case "google" -> user.getAttribute("sub");
            default       -> user.getAttribute("id");
        };
        return id != null ? id.toString() : null;
    }

    // ── User helpers ──────────────────────────────────────────────────────────

    private User buildNewOAuthUser(String provider, String email, String name, String avatar) {
        String[] parts = splitName(name);
        return User.builder()
                .email(email)
                .username(uniqueUsername(email, provider))
                // OAuth users have no password — set a random unguessable hash
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName(parts[0])
                .lastName(parts[1])
                .profileImageUrl(avatar)
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)   // provider has already verified the email
                .isLocked(false)
                .lastPasswordChange(LocalDateTime.now())
                .build();
    }

    private void patchExistingUser(User user, String name, String avatar) {
        if (name != null) {
            String[] parts = splitName(name);
            if (user.getFirstName() == null) user.setFirstName(parts[0]);
            if (user.getLastName()  == null) user.setLastName(parts[1]);
        }
        if (avatar != null && user.getProfileImageUrl() == null) {
            user.setProfileImageUrl(avatar);
        }
        // Mark email verified for any existing account that logs in via OAuth
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            user.setIsEmailVerified(true);
        }
    }

    private void upsertLinkedIdentity(UUID userId, String provider,
                                       String providerId, String email,
                                       String name, String avatar) {
        if (providerId == null) return;
        linkedIdentityRepository.findByProviderAndProviderUserId(provider, providerId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setDisplayName(name);
                            existing.setAvatarUrl(avatar);
                            linkedIdentityRepository.save(existing);
                        },
                        () -> linkedIdentityRepository.save(LinkedIdentity.builder()
                                .userId(userId)
                                .provider(provider)
                                .providerUserId(providerId)
                                .email(email)
                                .displayName(name)
                                .avatarUrl(avatar)
                                .build()));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String[] splitName(String name) {
        if (name != null && name.contains(" ")) return name.split(" ", 2);
        return new String[]{ name != null ? name : "User", "" };
    }

    private String uniqueUsername(String email, String provider) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String suffix = provider.substring(0, Math.min(3, provider.length()));
        String candidate = base + "_" + suffix;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix + "_" + UUID.randomUUID().toString().substring(0, 5);
        }
        return candidate;
    }
}

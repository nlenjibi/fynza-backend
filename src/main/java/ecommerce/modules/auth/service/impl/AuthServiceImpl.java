package ecommerce.modules.auth.service.impl;

import ecommerce.common.config.TokenProperties;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.event.user.PasswordResetRequestedEvent;
import ecommerce.common.event.user.UserRegisteredEvent;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.DuplicateResourceException;
import ecommerce.common.exception.InvalidTokenException;
import ecommerce.common.security.TotpUtil;
import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.LinkedIdentityResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.MfaSetupResponse;
import ecommerce.modules.auth.dto.RegisterRequest;
import ecommerce.modules.auth.dto.SessionResponse;
import ecommerce.modules.auth.repository.LinkedIdentityRepository;
import ecommerce.modules.auth.entity.Auth;
import ecommerce.modules.auth.entity.VerificationToken;
import ecommerce.modules.auth.entity.VerificationTokenType;
import ecommerce.modules.auth.repository.AuthRepository;
import ecommerce.modules.auth.repository.VerificationTokenRepository;
import ecommerce.modules.auth.service.AuthService;
import ecommerce.common.security.JwtTokenProvider;
import ecommerce.common.security.LoginAttemptService;
import ecommerce.common.security.SecurityEventLogger;
import ecommerce.modules.user.entity.CustomerProfile;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.CustomerProfileRepository;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String AUTH_PROVIDER_PASSWORD = "PASSWORD";
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final AuthRepository authRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final LinkedIdentityRepository linkedIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProperties tokenProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final SecurityEventLogger securityEventLogger;
    private final FynzaEventPublisher eventPublisher;


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        Role role = Role.CUSTOMER;
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("SELLER")) {
            role = Role.SELLER;
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(generateUsername(request.getEmail(), request.getFirstName(), request.getLastName()))
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(false)
                .lastPasswordChange(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        if (role == Role.SELLER) {
            SellerProfile sellerProfile = SellerProfile.builder()
                    .user(user)
                    .storeName(request.getFirstName() + "'s Store")
                    .verificationStatus(ecommerce.common.enums.VerificationStatus.PENDING)
                    .build();
            sellerProfileRepository.save(sellerProfile);
        } else {
            CustomerProfile customerProfile = CustomerProfile.builder()
                    .user(user)
                    .loyaltyPoints(0)
                    .membershipStatus(ecommerce.common.enums.MembershipStatus.BRONZE)
                    .totalOrders(0)
                    .totalSpent(java.math.BigDecimal.ZERO)
                    .build();
            customerProfileRepository.save(customerProfile);
        }

        String verificationToken = generateSecureToken();
        verificationTokenRepository.save(VerificationToken.builder()
                .userId(user.getId())
                .token(verificationToken)
                .tokenType(VerificationTokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(tokenProperties.getEmailVerificationHours()))
                .build());

        eventPublisher.publish(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                user.getRole(), false, verificationToken));

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        persistSession(user, accessToken, refreshToken);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        HttpServletRequest req = currentRequest();
        String ip = extractIp(req);

        if (loginAttemptService.isLocked(request.getEmail())) {
            log.warn("Blocked login attempt for locked account: {}", request.getEmail());
            securityEventLogger.logAccountLockout(request.getEmail(), ip, loginAttemptService.getAttempts(request.getEmail()));
            throw new BadRequestException("Account is locked due to too many failed attempts. Please try again in 15 minutes.");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getEmail());
            securityEventLogger.logLoginAttempt(request.getEmail(), ip, extractUserAgent(req), false, AUTH_PROVIDER_PASSWORD, "Invalid credentials");
            throw new BadRequestException(INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        loginAttemptService.loginSucceeded(request.getEmail());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        securityEventLogger.logLoginAttempt(request.getEmail(), ip, extractUserAgent(req), true, AUTH_PROVIDER_PASSWORD, null);

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            String challengeToken = generateSecureToken();
            verificationTokenRepository.save(VerificationToken.builder()
                    .userId(user.getId())
                    .token(challengeToken)
                    .tokenType(VerificationTokenType.MFA_CHALLENGE)
                    .expiresAt(LocalDateTime.now().plusMinutes(tokenProperties.getMfaChallengeMinutes()))
                    .build());
            log.info("MFA challenge issued for user: {}", user.getEmail());
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .mfaChallengeToken(challengeToken)
                    .build();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        persistSession(user, accessToken, refreshToken);

        log.info("User logged in successfully: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        Auth session = authRepository.findByRefreshTokenAndIsActiveTrue(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token session not found or revoked"));

        if (session.isExpired()) {
            session.setIsActive(false);
            authRepository.save(session);
            throw new InvalidTokenException("Refresh token has expired");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        // Rotate: invalidate old session, issue new tokens
        session.setIsActive(false);
        session.setLoggedOutAt(LocalDateTime.now());
        authRepository.save(session);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        persistSession(user, newAccessToken, newRefreshToken);
        securityEventLogger.logTokenRefresh(user.getPublicId(), user.getEmail(), true);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(UUID userId, String refreshToken) {
        log.info("Logging out user: {}", userId);

        if (refreshToken != null) {
            authRepository.findByRefreshTokenAndIsActiveTrue(refreshToken)
                    .ifPresent(session -> {
                        session.setIsActive(false);
                        session.setLoggedOutAt(LocalDateTime.now());
                        authRepository.save(session);
                        log.info("Session invalidated for user: {}", userId);
                    });
        }

        userRepository.findByPublicId(userId).ifPresent(user ->
                securityEventLogger.logLogout(userId, user.getEmail(), extractIp(currentRequest()))
        );

        log.info("User logged out successfully: {}", userId);
    }

    @Override
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Cleaning up expired sessions");
        int count = authRepository.invalidateExpiredSessions(LocalDateTime.now());
        log.info("Invalidated {} expired sessions", count);
    }

    @Override
    @Transactional
    public AuthResponse oauth2Login(User user, HttpServletRequest request) {
        log.info("OAuth2 login for user: {}", user.getEmail());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), "GOOGLE");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        Auth session = Auth.builder()
                .user(user)
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProperties.refreshMillis() / 1000))
                .lastActivityAt(LocalDateTime.now())
                .ipAddress(extractIp(request))
                .userAgent(extractUserAgent(request))
                .deviceName(resolveDeviceName(request))
                .build();
        authRepository.save(session);

        securityEventLogger.logLoginAttempt(user.getEmail(), extractIp(request), extractUserAgent(request), true, "GOOGLE", null);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = verificationTokenRepository
                .findByTokenAndTokenType(token, VerificationTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));

        if (!vt.isValid()) {
            throw new InvalidTokenException("Verification token has expired or already been used");
        }

        User user = userRepository.findByPublicId(vt.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setIsEmailVerified(true);
        userRepository.save(user);

        vt.setIsUsed(true);
        vt.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(vt);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found with that email"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BadRequestException("Email is already verified");
        }

        verificationTokenRepository.invalidateByUserIdAndType(user.getId(), VerificationTokenType.EMAIL_VERIFICATION);

        String token = generateSecureToken();
        verificationTokenRepository.save(VerificationToken.builder()
                .userId(user.getId())
                .token(token)
                .tokenType(VerificationTokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(tokenProperties.getEmailVerificationHours()))
                .build());

        eventPublisher.publish(new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                user.getRole(), false, token));

        log.info("Verification email resent for: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            verificationTokenRepository.invalidateByUserIdAndType(user.getId(), VerificationTokenType.PASSWORD_RESET);

            String token = generateSecureToken();
            verificationTokenRepository.save(VerificationToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .tokenType(VerificationTokenType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusMinutes(tokenProperties.getPasswordResetMinutes()))
                    .build());

            eventPublisher.publish(new PasswordResetRequestedEvent(
                    user.getId(), user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                    token, tokenProperties.getPasswordResetMinutes()));

            log.info("Password reset requested for user: {}", email);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }

        VerificationToken vt = verificationTokenRepository
                .findByTokenAndTokenType(token, VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!vt.isValid()) {
            throw new InvalidTokenException("Reset token has expired or already been used");
        }

        User user = userRepository.findByPublicId(vt.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        userRepository.save(user);

        vt.setIsUsed(true);
        vt.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(vt);

        authRepository.invalidateAllUserSessions(user.getId(), LocalDateTime.now());

        log.info("Password reset completed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        userRepository.save(user);

        authRepository.invalidateAllUserSessions(userId, LocalDateTime.now());

        log.info("Password changed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new BadRequestException("MFA is already enabled");
        }

        String secret = TotpUtil.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrUri = TotpUtil.generateQrUri("Fynza", user.getEmail(), secret);
        log.info("MFA setup initiated for user: {}", user.getEmail());

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrUri)
                .manualEntryCode(secret)
                .build();
    }

    @Override
    @Transactional
    public void enableMfa(UUID userId, String totpCode) {
        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getMfaSecret() == null) {
            throw new BadRequestException("MFA setup not initiated. Call /mfa/setup first.");
        }

        if (!TotpUtil.verify(user.getMfaSecret(), totpCode)) {
            throw new BadRequestException("Invalid TOTP code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("MFA enabled for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void disableMfa(UUID userId, String totpCode) {
        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new BadRequestException("MFA is not enabled");
        }

        if (!TotpUtil.verify(user.getMfaSecret(), totpCode)) {
            throw new BadRequestException("Invalid TOTP code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        log.info("MFA disabled for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public AuthResponse verifyMfa(String challengeToken, String totpCode) {
        VerificationToken vt = verificationTokenRepository
                .findByTokenAndTokenType(challengeToken, VerificationTokenType.MFA_CHALLENGE)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired MFA challenge"));

        if (!vt.isValid()) {
            throw new InvalidTokenException("MFA challenge has expired or already been used");
        }

        User user = userRepository.findByPublicId(vt.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!TotpUtil.verify(user.getMfaSecret(), totpCode)) {
            throw new BadRequestException("Invalid TOTP code");
        }

        vt.setIsUsed(true);
        vt.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(vt);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        persistSession(user, accessToken, refreshToken);
        log.info("MFA verified, tokens issued for user: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public List<LinkedIdentityResponse> listLinkedIdentities(UUID userId) {
        return linkedIdentityRepository.findAllByUserId(userId).stream()
                .map(li -> LinkedIdentityResponse.builder()
                        .provider(li.getProvider())
                        .displayName(li.getDisplayName())
                        .email(li.getEmail())
                        .avatarUrl(li.getAvatarUrl())
                        .linkedAt(li.getLinkedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void unlinkSocialAccount(UUID userId, String provider) {
        if (!linkedIdentityRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new BadRequestException("Provider '" + provider + "' is not linked to this account");
        }

        long count = linkedIdentityRepository.countByUserId(userId);
        if (count <= 1) {
            throw new BadRequestException(
                    "Cannot unlink your only social account. Set a password first using the forgot-password flow.");
        }

        linkedIdentityRepository.deleteByUserIdAndProvider(userId, provider);
        log.info("Unlinked provider='{}' for user={}", provider, userId);
    }

    @Override
    public List<SessionResponse> listActiveSessions(UUID userId, String currentRefreshToken) {
        return authRepository.findAllByUser_IdAndIsActiveTrueOrderByLastActivityAtDesc(userId)
                .stream()
                .map(s -> SessionResponse.builder()
                        .sessionId(s.getId())
                        .deviceName(s.getDeviceName())
                        .ipAddress(s.getIpAddress())
                        .createdAt(s.getCreatedAt())
                        .lastActivityAt(s.getLastActivityAt())
                        .expiresAt(s.getExpiresAt())
                        .current(s.getRefreshToken().equals(currentRefreshToken))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        Auth session = authRepository.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("Session does not belong to the current user");
        }

        if (!Boolean.TRUE.equals(session.getIsActive())) {
            throw new BadRequestException("Session is already inactive");
        }

        session.setIsActive(false);
        session.setLoggedOutAt(LocalDateTime.now());
        authRepository.save(session);

        log.info("Session {} revoked for user {}", sessionId, userId);
    }

    @Override
    @Transactional
    public void revokeAllOtherSessions(UUID userId, String currentRefreshToken) {
        int count = authRepository.invalidateOtherUserSessions(userId, currentRefreshToken, LocalDateTime.now());
        log.info("Revoked {} other sessions for user {}", count, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void persistSession(User user, String accessToken, String refreshToken) {
        HttpServletRequest req = currentRequest();
        Auth session = Auth.builder()
                .user(user)
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProperties.refreshMillis() / 1000))
                .lastActivityAt(LocalDateTime.now())
                .ipAddress(extractIp(req))
                .userAgent(extractUserAgent(req))
                .deviceName(resolveDeviceName(req))
                .build();
        authRepository.save(session);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userId(user.getPublicId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProperties.accessMillis() / 1000)
                .build();
    }

    private HttpServletRequest currentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private String extractIp(HttpServletRequest req) {
        if (req == null) return null;
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest req) {
        return req != null ? req.getHeader("User-Agent") : null;
    }

    private String resolveDeviceName(HttpServletRequest req) {
        if (req == null) return "Unknown";
        String ua = req.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Mobile")) return "Mobile";
        if (ua.contains("Tablet")) return "Tablet";
        return "Desktop";
    }

    private String generateUsername(String email, String firstName, String lastName) {
        String base = firstName != null && !firstName.isEmpty() ? firstName : email.split("@")[0];
        if (lastName != null && !lastName.isEmpty()) {
            base += "." + lastName;
        }
        String cleaned = base.toLowerCase().replaceAll("[^a-z0-9.]", "");
        return cleaned + "_" + System.currentTimeMillis();
    }
}

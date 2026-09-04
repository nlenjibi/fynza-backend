package ecommerce.modules.auth.service.impl;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.DuplicateResourceException;
import ecommerce.common.exception.InvalidTokenException;
import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.RegisterRequest;
import ecommerce.modules.auth.entity.Auth;
import ecommerce.modules.auth.repository.AuthRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String AUTH_PROVIDER_PASSWORD = "PASSWORD";
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final SecurityEventLogger securityEventLogger;

    @Value("${jwt.access-token.expiration:900000}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:604800000}")
    private Long refreshTokenExpiration;

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

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        persistSession(user, accessToken, refreshToken);
        securityEventLogger.logLoginAttempt(request.getEmail(), ip, extractUserAgent(req), true, AUTH_PROVIDER_PASSWORD, null);

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
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .lastActivityAt(LocalDateTime.now())
                .ipAddress(extractIp(request))
                .userAgent(extractUserAgent(request))
                .deviceName(resolveDeviceName(request))
                .build();
        authRepository.save(session);

        securityEventLogger.logLoginAttempt(user.getEmail(), extractIp(request), extractUserAgent(request), true, "GOOGLE", null);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void persistSession(User user, String accessToken, String refreshToken) {
        HttpServletRequest req = currentRequest();
        Auth session = Auth.builder()
                .user(user)
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
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
                .expiresIn(accessTokenExpiration / 1000)
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

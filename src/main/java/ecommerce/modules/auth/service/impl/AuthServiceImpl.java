package ecommerce.modules.auth.service.impl;

import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.DuplicateResourceException;
import ecommerce.common.exception.InvalidTokenException;
import ecommerce.modules.auth.dto.AuthResponse;
import ecommerce.modules.auth.dto.LoginRequest;
import ecommerce.modules.auth.dto.RegisterRequest;
import ecommerce.modules.auth.service.AuthService;
import ecommerce.common.util.TokenValidationService;
import ecommerce.common.security.JwtTokenProvider;
import ecommerce.modules.user.entity.CustomerProfile;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.CustomerProfileRepository;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.common.util.CacheStatisticsService;
import ecommerce.common.util.DatabaseMetricsService;
import ecommerce.common.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String AUTH_PROVIDER_PASSWORD = "PASSWORD";

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheStatisticsService cacheStatisticsService;
    private final DatabaseMetricsService.LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;
    private final TokenValidationService tokenValidationService;
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

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
            log.info("SellerProfile created for user: {}", user.getId());
        } else {
            CustomerProfile customerProfile = CustomerProfile.builder()
                    .user(user)
                    .loyaltyPoints(0)
                    .membershipStatus(ecommerce.common.enums.MembershipStatus.BRONZE)
                    .totalOrders(0)
                    .totalSpent(java.math.BigDecimal.ZERO)
                    .build();
            customerProfileRepository.save(customerProfile);
            log.info("CustomerProfile created for user: {}", user.getId());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

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

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        if (loginAttemptService.isLocked(request.getEmail())) {
            log.warn("Blocked login attempt for locked account: {}", request.getEmail());
            throw new BadRequestException("Account is locked due to too many failed attempts. Please try again in 15 minutes.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getEmail());
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

        log.info("User logged in successfully: {}", user.getEmail());

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

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), AUTH_PROVIDER_PASSWORD);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

        return AuthResponse.builder()
                .userId(user.getPublicId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId, String token) {
        log.info("Logging out user: {}", userId);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            long remainingTime = jwtTokenProvider.getTokenRemainingTime(token);
            if (remainingTime > 0) {
                tokenBlacklistService.blacklistToken(token, remainingTime);
                log.info("Token blacklisted for user: {}", userId);
            }
        }

        log.info("User logged out successfully: {}", userId);
    }

    @Override
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Cleaning up expired sessions");
    }

    @Override
    @Transactional
    public AuthResponse oauth2Login(User user, jakarta.servlet.http.HttpServletRequest request) {
        log.info("OAuth2 login for user: {}", user.getEmail());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getPublicId(), user.getEmail(), user.getRole().name(), "GOOGLE");
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getPublicId());

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
    private String generateUsername(String email, String firstName, String lastName) {
        String base = firstName != null && !firstName.isEmpty() ? firstName : email.split("@")[0];
        if (lastName != null && !lastName.isEmpty()) {
            base += "." + lastName;
        }
        String cleaned = base.toLowerCase().replaceAll("[^a-z0-9.]", "");
        return cleaned + "_" + System.currentTimeMillis();
    }
}

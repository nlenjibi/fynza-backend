package ecommerce.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider Unit Tests
 * 
 * Tests JWT token generation, validation, and extraction.
 * Per system.md: JWT Access Token = 15 min, Refresh Token = 7 days
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String secretKey = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 900000L); // 15 minutes
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L); // 7 days
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate access token")
        void generateAccessToken_ReturnsValidToken() {
            // Arrange
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";
            String role = "CUSTOMER";

            // Act
            String token = jwtTokenProvider.generateAccessToken(userId, email, role);

            // Assert
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Should generate refresh token")
        void generateRefreshToken_ReturnsValidToken() {
            // Arrange
            UUID userId = UUID.randomUUID();

            // Act
            String token = jwtTokenProvider.generateRefreshToken(userId);

            // Assert
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3);
        }

        @Test
        @DisplayName("Should generate token with correct claims")
        void generateToken_ContainsCorrectClaims() {
            // Arrange
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";
            String role = "ADMIN";

            // Act
            String token = jwtTokenProvider.generateAccessToken(userId, email, role);

            // Assert
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertEquals(userId.toString(), claims.getSubject());
            assertEquals(email, claims.get("email"));
            assertEquals(role, claims.get("role"));
            assertEquals("access", claims.get("type"));
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());
        }

        @Test
        @DisplayName("Should identify access token correctly")
        void isAccessToken_ReturnsTrueForAccessToken() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test@example.com", "CUSTOMER");

            // Act
            boolean isAccess = jwtTokenProvider.isAccessToken(token);

            // Assert
            assertTrue(isAccess);
            assertFalse(jwtTokenProvider.isRefreshToken(token));
        }

        @Test
        @DisplayName("Should identify refresh token correctly")
        void isRefreshToken_ReturnsTrueForRefreshToken() {
            // Arrange
            String token = jwtTokenProvider.generateRefreshToken(UUID.randomUUID());

            // Act
            boolean isRefresh = jwtTokenProvider.isRefreshToken(token);

            // Assert
            assertTrue(isRefresh);
            assertFalse(jwtTokenProvider.isAccessToken(token));
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid access token")
        void validateToken_WithValidToken_ReturnsTrue() {
            // Arrange
            UUID userId = UUID.randomUUID();
            String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com", "CUSTOMER");

            // Act
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Assert
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should invalidate expired token")
        void validateToken_WithExpiredToken_ReturnsFalse() {
            // Arrange - create expired token manually
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Date expiredDate = new Date(System.currentTimeMillis() - 1000);
            
            String token = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .claim("email", "test@example.com")
                    .claim("role", "CUSTOMER")
                    .claim("type", "access")
                    .issuedAt(expiredDate)
                    .expiration(expiredDate)
                    .signWith(key, Jwts.SIG.HS512)
                    .compact();

            // Act
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Assert
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should invalidate malformed token")
        void validateToken_WithMalformedToken_ReturnsFalse() {
            // Arrange
            String malformedToken = "not-a-valid-jwt-token";

            // Act
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // Assert
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should invalidate null token")
        void validateToken_WithNullToken_ReturnsFalse() {
            // Act
            boolean isValid = jwtTokenProvider.validateToken(null);

            // Assert
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("Token Extraction")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract user ID from token")
        void getUserId_FromValidToken_ReturnsUserId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            String token = jwtTokenProvider.generateAccessToken(userId, "test@example.com", "CUSTOMER");

            // Act
            UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // Assert
            assertEquals(userId, extractedUserId);
        }

        @Test
        @DisplayName("Should extract role from token")
        void getRole_FromValidToken_ReturnsRole() {
            // Arrange
            String role = "ADMIN";
            String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test@example.com", role);

            // Act
            String extractedRole = jwtTokenProvider.getRoleFromToken(token);

            // Assert
            assertEquals(role, extractedRole);
        }

        @Test
        @DisplayName("Should extract token type from token")
        void getTokenType_FromValidToken_ReturnsType() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test@example.com", "CUSTOMER");

            // Act
            String tokenType = jwtTokenProvider.getTokenType(token);

            // Assert
            assertEquals("access", tokenType);
        }

        @Test
        @DisplayName("Should extract expiration date from token")
        void getExpirationDate_FromValidToken_ReturnsDate() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test@example.com", "CUSTOMER");

            // Act
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);

            // Assert
            assertNotNull(expiration);
            assertTrue(expiration.after(new Date()));
        }

        @Test
        @DisplayName("Should get remaining time for token")
        void getTokenRemainingTime_ReturnsPositiveValue() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test@example.com", "CUSTOMER");

            // Act
            long remainingTime = jwtTokenProvider.getTokenRemainingTime(token);

            // Assert
            assertTrue(remainingTime > 0);
        }
    }

    @Nested
    @DisplayName("Token Expiry")
    class TokenExpiryTests {

        @Test
        @DisplayName("Access token should expire in 15 minutes")
        void accessToken_ExpiresIn15Minutes() {
            // Arrange
            String token = jwtTokenProvider.generateAccessToken(UUID.randomUUID(), "test@example.com", "CUSTOMER");

            // Act
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            long expiryMillis = expiration.getTime() - System.currentTimeMillis();
            long expiryMinutes = expiryMillis / 1000 / 60;

            // Assert - should be approximately 15 minutes (900000ms)
            assertTrue(expiryMinutes >= 14 && expiryMinutes <= 15);
        }

        @Test
        @DisplayName("Refresh token should expire in 7 days")
        void refreshToken_ExpiresIn7Days() {
            // Arrange
            String token = jwtTokenProvider.generateRefreshToken(UUID.randomUUID());

            // Act
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            long expiryMillis = expiration.getTime() - System.currentTimeMillis();
            long expiryDays = expiryMillis / 1000 / 60 / 60 / 24;

            // Assert - should be approximately 7 days
            assertTrue(expiryDays >= 6 && expiryDays <= 7);
        }

        @Test
        @DisplayName("Should return configured access token expiration")
        void getAccessTokenExpiration_ReturnsConfiguredValue() {
            // Act
            long expiration = jwtTokenProvider.getAccessTokenExpiration();

            // Assert
            assertEquals(900000L, expiration);
        }
    }
}

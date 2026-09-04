package ecommerce.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TokenBlacklistService Tests")
class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;
    private BloomFilterService bloomFilterService;

    @BeforeEach
    void setUp() {
        bloomFilterService = mock(BloomFilterService.class);
        tokenBlacklistService = new TokenBlacklistService(10000, 24, bloomFilterService);
    }

    @Nested
    @DisplayName("Token Blacklisting")
    class TokenBlacklistTests {

        @Test
        @DisplayName("Should add token to blacklist")
        void blacklistToken_ValidToken_AddsSuccessfully() {
            String token = "test-jwt-token-123";
            long expirationTime = System.currentTimeMillis() + 3600000;

            tokenBlacklistService.blacklistToken(token, expirationTime);

            assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
            verify(bloomFilterService).add(anyString());
        }

        @Test
        @DisplayName("Should check if token is blacklisted")
        void isTokenBlacklisted_BlacklistedToken_ReturnsTrue() {
            String token = "another-test-token";
            long expirationTime = System.currentTimeMillis() + 3600000;
            tokenBlacklistService.blacklistToken(token, expirationTime);

            when(bloomFilterService.mightContain(anyString())).thenReturn(true);

            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);

            assertTrue(isBlacklisted);
        }

        @Test
        @DisplayName("Should return false for non-blacklisted token")
        void isTokenBlacklisted_NonBlacklistedToken_ReturnsFalse() {
            String token = "non-blacklisted-token";

            when(bloomFilterService.mightContain(anyString())).thenReturn(false);

            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);

            assertFalse(isBlacklisted);
        }

        @Test
        @DisplayName("Should return false for null token")
        void isTokenBlacklisted_NullToken_ReturnsFalse() {
            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(null);

            assertFalse(isBlacklisted);
        }

        @Test
        @DisplayName("Should handle empty token")
        void isTokenBlacklisted_EmptyToken_ReturnsFalse() {
            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted("");

            assertFalse(isBlacklisted);
        }
    }

    @Nested
    @DisplayName("Token Version Management")
    class TokenVersionTests {

        @Test
        @DisplayName("Should invalidate user tokens by version")
        void invalidateUserTokens_InvalidatesAllTokens() {
            UUID userId = UUID.randomUUID();

            tokenBlacklistService.invalidateUserTokens(userId);

            Long currentVersion = tokenBlacklistService.getUserTokenVersion(userId);
            assertNotNull(currentVersion);
        }

        @Test
        @DisplayName("Should get token version for user")
        void getUserTokenVersion_ReturnsCurrentVersion() {
            UUID userId = UUID.randomUUID();
            tokenBlacklistService.invalidateUserTokens(userId);

            Long retrievedVersion = tokenBlacklistService.getUserTokenVersion(userId);

            assertNotNull(retrievedVersion);
        }

        @Test
        @DisplayName("Should return null for non-existent user version")
        void getUserTokenVersion_NonExistentUser_ReturnsNull() {
            UUID userId = UUID.randomUUID();

            Long version = tokenBlacklistService.getUserTokenVersion(userId);

            assertNull(version);
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperationTests {

        @Test
        @DisplayName("Should add multiple tokens to blacklist")
        void blacklistMultipleTokens_AddsAllTokens() {
            String[] tokens = {"token1", "token2", "token3"};
            long expirationTime = System.currentTimeMillis() + 3600000;

            for (String token : tokens) {
                tokenBlacklistService.blacklistToken(token, expirationTime);
            }

            when(bloomFilterService.mightContain(anyString())).thenReturn(true);

            assertTrue(tokenBlacklistService.isTokenBlacklisted("token1"));
            assertTrue(tokenBlacklistService.isTokenBlacklisted("token2"));
            assertTrue(tokenBlacklistService.isTokenBlacklisted("token3"));
        }

        @Test
        @DisplayName("Should clear expired tokens")
        void clearExpiredTokens_RemovesExpiredTokens() {
            tokenBlacklistService.clearExpiredTokens();
        }
    }

    @Nested
    @DisplayName("Token Version Validation")
    class TokenVersionValidationTests {

        @Test
        @DisplayName("Should validate token version")
        void isUserTokenVersionValid_ValidVersion_ReturnsTrue() {
            UUID userId = UUID.randomUUID();
            tokenBlacklistService.invalidateUserTokens(userId);
            Long currentVersion = tokenBlacklistService.getUserTokenVersion(userId);

            boolean isValid = tokenBlacklistService.isUserTokenVersionValid(userId, currentVersion);

            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should invalidate old token version")
        void isUserTokenVersionValid_OldVersion_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            tokenBlacklistService.invalidateUserTokens(userId);
            Long oldVersion = tokenBlacklistService.getUserTokenVersion(userId);
            
            tokenBlacklistService.invalidateUserTokens(userId);

            boolean isValid = tokenBlacklistService.isUserTokenVersionValid(userId, oldVersion);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should return true for null version on new user")
        void isUserTokenVersionValid_NullVersion_NewUser_ReturnsTrue() {
            UUID userId = UUID.randomUUID();

            boolean isValid = tokenBlacklistService.isUserTokenVersionValid(userId, null);

            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should return false for non-existent user with version")
        void isUserTokenVersionValid_NonExistentUser_WithVersion_ReturnsTrue() {
            UUID userId = UUID.randomUUID();

            boolean isValid = tokenBlacklistService.isUserTokenVersionValid(userId, 12345L);

            assertTrue(isValid);
        }
    }
}

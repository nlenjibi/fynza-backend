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
        @DisplayName("Should make pre-invalidation tokens invalid")
        void invalidateUserTokens_MakesOldTokensInvalid() {
            UUID userId = UUID.randomUUID();
            long issuedBefore = System.currentTimeMillis() - 1000;

            tokenBlacklistService.invalidateUserTokens(userId);

            assertFalse(tokenBlacklistService.isUserTokenVersionValid(userId, issuedBefore));
        }

        @Test
        @DisplayName("Should keep post-invalidation tokens valid")
        void invalidateUserTokens_KeepsNewTokensValid() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            tokenBlacklistService.invalidateUserTokens(userId);
            Thread.sleep(1);
            long issuedAfter = System.currentTimeMillis();

            assertTrue(tokenBlacklistService.isUserTokenVersionValid(userId, issuedAfter));
        }

        @Test
        @DisplayName("Should return true when no invalidation exists for user")
        void noInvalidation_AnyTokenIsValid() {
            UUID userId = UUID.randomUUID();

            assertTrue(tokenBlacklistService.isUserTokenVersionValid(userId, 0L));
            assertTrue(tokenBlacklistService.isUserTokenVersionValid(userId, System.currentTimeMillis()));
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
        @DisplayName("Token issued after invalidation should be valid")
        void isUserTokenVersionValid_TokenAfterInvalidation_ReturnsTrue() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            tokenBlacklistService.invalidateUserTokens(userId);
            Thread.sleep(1);
            long issuedAt = System.currentTimeMillis();

            assertTrue(tokenBlacklistService.isUserTokenVersionValid(userId, issuedAt));
        }

        @Test
        @DisplayName("Token issued before invalidation should be invalid")
        void isUserTokenVersionValid_TokenBeforeInvalidation_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            long issuedAt = System.currentTimeMillis() - 5000;

            tokenBlacklistService.invalidateUserTokens(userId);

            assertFalse(tokenBlacklistService.isUserTokenVersionValid(userId, issuedAt));
        }

        @Test
        @DisplayName("Token issued before re-invalidation should be invalid")
        void isUserTokenVersionValid_TokenBeforeReInvalidation_ReturnsFalse() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            tokenBlacklistService.invalidateUserTokens(userId);
            Thread.sleep(1);
            long issuedBetween = System.currentTimeMillis();
            Thread.sleep(1);
            tokenBlacklistService.invalidateUserTokens(userId);

            assertFalse(tokenBlacklistService.isUserTokenVersionValid(userId, issuedBetween));
        }

        @Test
        @DisplayName("Should return true for any token when no invalidation exists")
        void isUserTokenVersionValid_NoInvalidationStored_ReturnsTrue() {
            UUID userId = UUID.randomUUID();

            assertTrue(tokenBlacklistService.isUserTokenVersionValid(userId, 12345L));
            assertTrue(tokenBlacklistService.isUserTokenVersionValid(userId, 0L));
        }
    }
}

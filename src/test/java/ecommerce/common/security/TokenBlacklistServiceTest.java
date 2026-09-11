package ecommerce.common.security;

import ecommerce.common.cache.CacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TokenBlacklistService Tests")
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private CacheProperties cacheProperties;
    @Mock private CacheProperties.Redis redisProps;

    private TokenBlacklistService service;
    private final Map<String, Object> redisStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(cacheProperties.getRedis()).thenReturn(redisProps);
        when(redisProps.getTokenBlacklistTtl()).thenReturn(Duration.ofHours(24));
        when(redisProps.getUserTokenVersionTtl()).thenReturn(Duration.ofHours(24));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        redisStore.clear();
        doAnswer(inv -> { redisStore.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(valueOps).set(anyString(), any(), any(Duration.class));
        doAnswer(inv -> redisStore.get(inv.<String>getArgument(0)))
                .when(valueOps).get(anyString());

        service = new TokenBlacklistService(redisTemplate, bloomFilterService, cacheProperties);
    }

    @Nested
    @DisplayName("Token Blacklisting")
    class TokenBlacklistTests {

        @Test
        @DisplayName("Should add token to blacklist")
        void blacklistToken_ValidToken_AddsSuccessfully() {
            String token = "test-jwt-token-123";
            long expiry = System.currentTimeMillis() + 3_600_000;
            when(bloomFilterService.mightContain(anyString())).thenReturn(true);

            service.blacklistToken(token, expiry);

            verify(bloomFilterService).add(anyString());
            verify(valueOps).set(anyString(), eq(Boolean.TRUE), any(Duration.class));
            assertTrue(service.isTokenBlacklisted(token));
        }

        @Test
        @DisplayName("Should return true for blacklisted token")
        void isTokenBlacklisted_BlacklistedToken_ReturnsTrue() {
            String token = "blacklisted-token";
            long expiry = System.currentTimeMillis() + 3_600_000;
            when(bloomFilterService.mightContain(anyString())).thenReturn(true);

            service.blacklistToken(token, expiry);

            assertTrue(service.isTokenBlacklisted(token));
        }

        @Test
        @DisplayName("Should return false when bloom filter clears token")
        void isTokenBlacklisted_NonBlacklistedToken_ReturnsFalse() {
            when(bloomFilterService.mightContain(anyString())).thenReturn(false);
            assertFalse(service.isTokenBlacklisted("non-blacklisted-token"));
            verifyNoInteractions(valueOps);
        }

        @Test
        @DisplayName("Should return false for null token")
        void isTokenBlacklisted_NullToken_ReturnsFalse() {
            assertFalse(service.isTokenBlacklisted(null));
            verifyNoInteractions(bloomFilterService, valueOps);
        }

        @Test
        @DisplayName("Should return false for empty token")
        void isTokenBlacklisted_EmptyToken_ReturnsFalse() {
            assertFalse(service.isTokenBlacklisted(""));
            verifyNoInteractions(bloomFilterService, valueOps);
        }

        @Test
        @DisplayName("Should fail-closed when Redis is unavailable")
        void isTokenBlacklisted_RedisUnavailable_ReturnsTrue() {
            when(bloomFilterService.mightContain(anyString())).thenReturn(true);
            doThrow(new RuntimeException("Redis down")).when(valueOps).get(anyString());
            assertTrue(service.isTokenBlacklisted("any-token"));
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

            service.invalidateUserTokens(userId);

            assertFalse(service.isUserTokenVersionValid(userId, issuedBefore));
        }

        @Test
        @DisplayName("Should keep post-invalidation tokens valid")
        void invalidateUserTokens_KeepsNewTokensValid() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            service.invalidateUserTokens(userId);
            Thread.sleep(1);
            long issuedAfter = System.currentTimeMillis();

            assertTrue(service.isUserTokenVersionValid(userId, issuedAfter));
        }

        @Test
        @DisplayName("Should return true when no invalidation exists")
        void noInvalidation_AnyTokenIsValid() {
            UUID userId = UUID.randomUUID();
            assertTrue(service.isUserTokenVersionValid(userId, 0L));
            assertTrue(service.isUserTokenVersionValid(userId, System.currentTimeMillis()));
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperationTests {

        @Test
        @DisplayName("Should add multiple tokens to blacklist")
        void blacklistMultipleTokens_AddsAllTokens() {
            long expiry = System.currentTimeMillis() + 3_600_000;
            when(bloomFilterService.mightContain(anyString())).thenReturn(true);

            service.blacklistToken("token1", expiry);
            service.blacklistToken("token2", expiry);
            service.blacklistToken("token3", expiry);

            assertTrue(service.isTokenBlacklisted("token1"));
            assertTrue(service.isTokenBlacklisted("token2"));
            assertTrue(service.isTokenBlacklisted("token3"));
        }

        @Test
        @DisplayName("clearExpiredTokens should complete without error")
        void clearExpiredTokens_IsNoOp() {
            assertDoesNotThrow(() -> service.clearExpiredTokens());
        }
    }

    @Nested
    @DisplayName("Token Version Validation")
    class TokenVersionValidationTests {

        @Test
        @DisplayName("Token issued after invalidation should be valid")
        void isUserTokenVersionValid_TokenAfterInvalidation_ReturnsTrue() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            service.invalidateUserTokens(userId);
            Thread.sleep(1);
            long issuedAt = System.currentTimeMillis();

            assertTrue(service.isUserTokenVersionValid(userId, issuedAt));
        }

        @Test
        @DisplayName("Token issued before invalidation should be invalid")
        void isUserTokenVersionValid_TokenBeforeInvalidation_ReturnsFalse() {
            UUID userId = UUID.randomUUID();
            long issuedAt = System.currentTimeMillis() - 5000;

            service.invalidateUserTokens(userId);

            assertFalse(service.isUserTokenVersionValid(userId, issuedAt));
        }

        @Test
        @DisplayName("Token issued before re-invalidation should be invalid")
        void isUserTokenVersionValid_TokenBeforeReInvalidation_ReturnsFalse() throws InterruptedException {
            UUID userId = UUID.randomUUID();
            service.invalidateUserTokens(userId);
            Thread.sleep(1);
            long issuedBetween = System.currentTimeMillis();
            Thread.sleep(1);
            service.invalidateUserTokens(userId);

            assertFalse(service.isUserTokenVersionValid(userId, issuedBetween));
        }

        @Test
        @DisplayName("Should return true for any token when no invalidation exists")
        void isUserTokenVersionValid_NoInvalidationStored_ReturnsTrue() {
            UUID userId = UUID.randomUUID();
            assertTrue(service.isUserTokenVersionValid(userId, 12345L));
            assertTrue(service.isUserTokenVersionValid(userId, 0L));
        }
    }

    @Nested
    @DisplayName("Stats")
    class StatsTests {

        @Test
        @DisplayName("getStats should return non-null stats with zero size when Redis returns null keys")
        void getStats_RedisReturnsNull_SizeIsZero() {
            when(redisTemplate.keys(anyString())).thenReturn(null);
            var stats = service.getStats();
            assertNotNull(stats);
            assertEquals(0L, stats.currentSize());
        }
    }
}

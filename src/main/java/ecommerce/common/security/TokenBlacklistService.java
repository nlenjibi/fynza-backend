package ecommerce.common.security;

import ecommerce.common.cache.CacheKey;
import ecommerce.common.cache.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Manages JWT token blacklist and per-user token versioning via Redis.
 *
 * <p>Security posture: <strong>fail-closed</strong>.
 * If Redis is unavailable during a blacklist check the token is treated as
 * blacklisted and the request is rejected, preventing use of revoked tokens
 * during a cache outage.
 *
 * <p>A Guava Bloom Filter provides an in-process O(1) pre-check to avoid
 * hitting Redis for tokens that are definitely not blacklisted.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BloomFilterService bloomFilterService;
    private final Duration blacklistTtl;
    private final Duration tokenVersionTtl;

    public TokenBlacklistService(
            RedisTemplate<String, Object> redisTemplate,
            BloomFilterService bloomFilterService,
            CacheProperties cacheProperties) {

        this.redisTemplate = redisTemplate;
        this.bloomFilterService = bloomFilterService;
        this.blacklistTtl = cacheProperties.getRedis().getTokenBlacklistTtl();
        this.tokenVersionTtl = cacheProperties.getRedis().getUserTokenVersionTtl();

        log.info("TokenBlacklistService initialised — blacklistTtl={}, tokenVersionTtl={}",
                blacklistTtl, tokenVersionTtl);
    }

    /**
     * Adds {@code token} to the Redis blacklist.
     * Fail-closed: propagates on Redis error (caller should handle / surface 500).
     */
    public void blacklistToken(String token, long expirationTime) {
        String hash = hashToken(token);
        String key  = CacheKey.tokenBlacklist(hash);

        long remainingMs = Math.max(expirationTime - System.currentTimeMillis(), 0);
        Duration ttl = remainingMs > 0 ? Duration.ofMillis(remainingMs) : blacklistTtl;

        redisTemplate.opsForValue().set(key, Boolean.TRUE, ttl);
        bloomFilterService.add(hash);

        log.debug("Token blacklisted — key={}, ttl={}", key.substring(0, Math.min(20, key.length())), ttl);
    }

    /**
     * Returns {@code true} if {@code token} is on the blacklist.
     *
     * <p>Fail-closed: any Redis error causes this method to return {@code true},
     * blocking the request to prevent use of revoked tokens during an outage.
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        String hash = hashToken(token);

        // Bloom Filter fast-path: definite NO avoids Redis entirely
        if (!bloomFilterService.mightContain(hash)) {
            return false;
        }

        String key = CacheKey.tokenBlacklist(hash);
        try {
            Boolean blacklisted = (Boolean) redisTemplate.opsForValue().get(key);
            if (Boolean.TRUE.equals(blacklisted)) {
                log.debug("Blacklisted token detected: {}", key.substring(0, Math.min(20, key.length())));
                return true;
            }
            return false;
        } catch (Exception e) {
            // Fail-closed: Redis unavailable → reject token for security
            log.error("Redis unavailable during blacklist check (fail-closed, rejecting token): {}", e.getMessage());
            return true;
        }
    }

    /**
     * Records a new token-version epoch for {@code userId}, invalidating
     * all previously issued tokens for that user.
     */
    public void invalidateUserTokens(UUID userId) {
        String key     = CacheKey.userTokenVersion(userId);
        long   version = System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, version, tokenVersionTtl);
        log.info("Invalidated all tokens for user: {}", userId);
    }

    /**
     * Returns {@code true} if {@code tokenVersion} is still valid (not superseded).
     */
    public boolean isUserTokenVersionValid(UUID userId, Long tokenVersion) {
        Long currentVersion = getUserTokenVersion(userId);
        if (currentVersion == null) return true;
        return tokenVersion == null || tokenVersion >= currentVersion;
    }

    /**
     * Returns the current token-version epoch for {@code userId}, or {@code null}
     * if no invalidation has been recorded.
     */
    public Long getUserTokenVersion(UUID userId) {
        try {
            Object value = redisTemplate.opsForValue().get(CacheKey.userTokenVersion(userId));
            if (value instanceof Long l) return l;
            if (value instanceof Integer i) return i.longValue();
            if (value instanceof Number n) return n.longValue();
            return null;
        } catch (Exception e) {
            log.warn("Failed to read user token version from Redis for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * No-op — Redis expires blacklisted tokens automatically via TTL.
     * Kept for scheduler compatibility.
     */
    public void clearExpiredTokens() {
        log.debug("clearExpiredTokens() called — Redis handles TTL expiry automatically");
    }

    /**
     * Returns basic stats about the blacklist.
     * Hit/miss rates are not tracked at the Redis level; size is estimated from key count.
     */
    public TokenBlacklistStats getStats() {
        long size = 0;
        try {
            var keys = redisTemplate.keys(CacheKey.of("token-blacklist", "*"));
            size = keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("Unable to estimate blacklist size from Redis: {}", e.getMessage());
        }
        return new TokenBlacklistStats(size, 0.0, 0.0);
    }

    public record TokenBlacklistStats(long currentSize, double hitRate, double missRate) {}

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available, falling back to hashCode", e);
            return String.valueOf(token.hashCode());
        }
    }
}

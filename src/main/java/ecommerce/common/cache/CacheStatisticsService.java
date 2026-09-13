package ecommerce.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides per-cache key counts (via non-blocking Redis SCAN) and
 * global hit/miss metrics (via Redis INFO stats).
 *
 * Redis INFO keyspace_hits / keyspace_misses are server-wide totals —
 * Redis does not expose per-prefix stats natively.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheStatisticsService {

    private final CacheManager cacheManager;
    private final StringRedisTemplate redis;

    public Map<String, CacheStats> getAllCacheStatistics() {
        Map<String, CacheStats> stats = new HashMap<>();
        GlobalRedisStats global = fetchGlobalStats();
        cacheManager.getCacheNames()
                .forEach(name -> stats.put(name, buildStats(name, global)));
        return stats;
    }

    public CacheStats getCacheStats(Cache cache) {
        if (cache == null) return null;
        return buildStats(cache.getName(), fetchGlobalStats());
    }

    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        log.info("All caches cleared");
    }

    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
        log.info("Cache '{}' cleared", cacheName);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private CacheStats buildStats(String cacheName, GlobalRedisStats global) {
        return new CacheStats(
                cacheName,
                global.hits(),
                global.misses(),
                global.hitRate(),
                countKeys(cacheName),
                global.evictedKeys()
        );
    }

    /**
     * Non-blocking SCAN to count keys in a single cache region.
     * Spring RedisCacheManager key format with prefix "fynza:" is: fynza:{cacheName}::{key}
     */
    private long countKeys(String cacheName) {
        String pattern = "fynza:" + cacheName + "::*";
        try {
            Long count = redis.execute((RedisCallback<Long>) connection -> {
                long c = 0;
                ScanOptions opts = ScanOptions.scanOptions()
                        .match(pattern).count(100).build();
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(opts)) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        c++;
                    }
                } catch (Exception e) {
                    log.warn("SCAN interrupted for cache '{}': {}", cacheName, e.getMessage());
                }
                return c;
            });
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Could not count keys for cache '{}': {}", cacheName, e.getMessage());
            return 0L;
        }
    }

    /**
     * Reads global keyspace_hits / keyspace_misses / evicted_keys from Redis INFO.
     */
    private GlobalRedisStats fetchGlobalStats() {
        try {
            Properties info = redis.execute(
                    (RedisCallback<Properties>) conn -> conn.serverCommands().info("stats"));
            if (info == null) return GlobalRedisStats.empty();

            long hits    = parseLong(info, "keyspace_hits");
            long misses  = parseLong(info, "keyspace_misses");
            long evicted = parseLong(info, "evicted_keys");
            double rate  = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
            return new GlobalRedisStats(hits, misses, rate, evicted);
        } catch (Exception e) {
            log.warn("Could not read Redis INFO stats: {}", e.getMessage());
            return GlobalRedisStats.empty();
        }
    }

    private static long parseLong(Properties props, String key) {
        return Long.parseLong(props.getProperty(key, "0"));
    }

    // ── records ───────────────────────────────────────────────────────────────

    public record CacheStats(
            String name,
            long   hitCount,
            long   missCount,
            double hitRate,
            long   size,
            long   evictionCount
    ) {}

    private record GlobalRedisStats(long hits, long misses, double hitRate, long evictedKeys) {
        static GlobalRedisStats empty() { return new GlobalRedisStats(0, 0, 0.0, 0); }
    }
}

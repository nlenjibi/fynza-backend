package ecommerce.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheStatisticsService {

    private final CacheManager cacheManager;

    public Map<String, CacheStats> getAllCacheStatistics() {
        Map<String, CacheStats> stats = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                stats.put(cacheName, getCacheStats(cache));
            }
        });
        return stats;
    }

    public CacheStats getCacheStats(Cache cache) {
        if (cache == null) return null;
        return new CacheStats(cache.getName());
    }

    public void clearAllCaches() {
        log.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public record CacheStats(String name) {}
}

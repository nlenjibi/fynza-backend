package ecommerce.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Degrades cache failures to cache misses instead of propagating exceptions.
 * On GET errors, evicts the potentially poisoned entry so subsequent requests
 * fetch fresh data from the source rather than hitting a broken cache entry.
 */
@Slf4j
@Component
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(@NonNull RuntimeException ex,
                                    @NonNull Cache cache,
                                    @NonNull Object key) {
        log.warn("Cache GET error in '{}' for key '{}' — treating as miss: {}",
                cache.getName(), key, ex.getMessage());
        try {
            cache.evictIfPresent(key);
        } catch (Exception evictEx) {
            log.debug("Failed to evict poisoned cache entry '{}' from '{}': {}",
                    key, cache.getName(), evictEx.getMessage());
        }
    }

    @Override
    public void handleCachePutError(@NonNull RuntimeException ex,
                                    @NonNull Cache cache,
                                    @NonNull Object key,
                                    Object value) {
        log.warn("Cache PUT error in '{}' for key '{}': {}", cache.getName(), key, ex.getMessage());
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException ex,
                                      @NonNull Cache cache,
                                      @NonNull Object key) {
        log.warn("Cache EVICT error in '{}' for key '{}': {}", cache.getName(), key, ex.getMessage());
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException ex,
                                      @NonNull Cache cache) {
        log.warn("Cache CLEAR error in '{}': {}", cache.getName(), ex.getMessage());
    }
}

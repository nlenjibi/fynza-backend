package ecommerce.modules.cache.impl;

import com.aoms.aomsbackend.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Redis-backed implementation of {@link RedisCacheService}.
 * <p>
 * Delegates all cache interactions to the injected Spring {@link CacheManager}
 * (typically backed by Redis) and swallows exceptions with error-level logging
 * so that cache failures never propagate to callers.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheServiceImpl implements RedisCacheService {

    private final CacheManager cacheManager;

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> get(String cacheName, Object key, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) return Optional.empty();
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper == null) {
                log.debug("Cache MISS [{} :: {}]", cacheName, key);
                return Optional.empty();
            }
            log.debug("Cache HIT [{} :: {}]", cacheName, key);
            return Optional.ofNullable(type.cast(wrapper.get()));
        } catch (Exception e) {
            log.error("Cache GET failed [{} :: {}]: {}", cacheName, key, e.getMessage());
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void put(String cacheName, Object key, Object value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.put(key, value);
        } catch (Exception e) {
            log.error("Cache PUT failed [{} :: {}]: {}", cacheName, key, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void evict(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.evict(key);
            log.debug("Cache EVICT [{} :: {}]", cacheName, key);
        } catch (Exception e) {
            log.error("Cache EVICT failed [{} :: {}]: {}", cacheName, key, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void evictAll(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.clear();
            log.debug("Cache CLEAR [{}]", cacheName);
        } catch (Exception e) {
            log.error("Cache CLEAR failed [{}]: {}", cacheName, e.getMessage());
        }
    }
}

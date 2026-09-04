package ecommerce.modules.cache;

import java.util.Optional;

/**
 * Contract for interacting with the Redis-backed cache in a type-safe manner.
 * <p>
 * Provides coarse-grained cache operations (get, put, evict, evict-all) keyed
 * by a named cache region and an arbitrary cache key, delegating to the
 * configured Spring {@link org.springframework.cache.CacheManager}.
 * </p>
 */
public interface RedisCacheService {

    /**
     * Retrieves a cached value by cache name and key, casting it to the expected type.
     *
     * @param <T>       the expected value type
     * @param cacheName the name of the cache region to look up
     * @param key       the cache key within the named region
     * @param type      the {@link Class} to cast the cached value to
     * @return an {@link Optional} containing the cached value, or empty on a cache miss or error
     */
    <T> Optional<T> get(String cacheName, Object key, Class<T> type);

    /**
     * Stores a value in the specified cache region under the given key.
     *
     * @param cacheName the name of the cache region
     * @param key       the cache key to associate with the value
     * @param value     the value to cache
     */
    void put(String cacheName, Object key, Object value);

    /**
     * Evicts a single entry from the specified cache region.
     *
     * @param cacheName the name of the cache region
     * @param key       the cache key of the entry to remove
     */
    void evict(String cacheName, Object key);

    /**
     * Clears all entries from the specified cache region.
     *
     * @param cacheName the name of the cache region to clear
     */
    void evictAll(String cacheName);
}

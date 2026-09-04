package ecommerce.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Redis Pub/Sub Listener for Cross-Instance Cache Invalidation
 * 
 * When any instance evicts a cache entry, it publishes a message.
 * All other instances receive this message and clear their local L1 cache.
 */
@Slf4j
@RequiredArgsConstructor
public class CacheInvalidationListener implements MessageListener {

    private final CacheManager cacheManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Received cache invalidation message: {}", payload);

            String[] parts = payload.split(":", 2);
            if (parts.length == 2) {
                String cacheName = parts[0];
                String key = parts[1];

                evictFromCache(cacheName, key);
            } else if (parts.length == 1) {
                String cacheName = parts[0];
                clearCache(cacheName);
            }

        } catch (Exception e) {
            log.error("Error processing cache invalidation message: {}", e.getMessage(), e);
        }
    }

    private void evictFromCache(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.info("Evicted key '{}' from cache '{}'", key, cacheName);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared all entries from cache '{}'", cacheName);
        }
    }
}

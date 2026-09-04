package ecommerce.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cache Invalidation Publisher
 * 
 * Publishes cache eviction messages to Redis Pub/Sub
 * All other instances subscribe and clear their local L1 cache
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    private static final String CHANNEL = "cache:invalidate";
    private final RedisTemplate<String, Object> redisTemplate;

    public void publishEvict(String cacheName, String key) {
        try {
            String message = cacheName + ":" + key;
            redisTemplate.convertAndSend(CHANNEL, message);
            log.debug("Published cache eviction: {}:{}", cacheName, key);
        } catch (Exception e) {
            log.warn("Failed to publish cache eviction for {}:{}: {}", cacheName, key, e.getMessage());
        }
    }

    public void publishClear(String cacheName) {
        try {
            redisTemplate.convertAndSend(CHANNEL, cacheName);
            log.debug("Published cache clear: {}", cacheName);
        } catch (Exception e) {
            log.warn("Failed to publish cache clear for {}: {}", cacheName, e.getMessage());
        }
    }
}

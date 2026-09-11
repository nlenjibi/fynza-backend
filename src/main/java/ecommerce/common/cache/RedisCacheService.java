package ecommerce.common.cache;

import ecommerce.common.cache.exception.CacheUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCircuitBreaker circuitBreaker;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            return circuitBreaker.execute(() -> {
                Object value = redisTemplate.opsForValue().get(key);
                if (value == null) return Optional.empty();
                if (type.isInstance(value)) return Optional.of(type.cast(value));
                log.warn("Cache type mismatch for key {}: expected {}, got {}",
                        key, type.getSimpleName(), value.getClass().getSimpleName());
                return Optional.empty();
            });
        } catch (CacheUnavailableException e) {
            log.warn("Cache unavailable (circuit open), miss for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Cache GET failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            circuitBreaker.executeVoid(() ->
                    redisTemplate.opsForValue().set(key, value, ttl));
        } catch (CacheUnavailableException e) {
            log.warn("Cache unavailable (circuit open), skipping PUT for key: {}", key);
        } catch (Exception e) {
            log.warn("Cache PUT failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        try {
            circuitBreaker.executeVoid(() -> redisTemplate.delete(key));
        } catch (CacheUnavailableException e) {
            log.warn("Cache unavailable (circuit open), skipping EVICT for key: {}", key);
        } catch (Exception e) {
            log.warn("Cache EVICT failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(circuitBreaker.execute(
                    () -> redisTemplate.hasKey(key)));
        } catch (CacheUnavailableException e) {
            log.warn("Cache unavailable (circuit open), exists check fails-open for key: {}", key);
            return false;
        } catch (Exception e) {
            log.warn("Cache EXISTS failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public void clear(String keyPattern) {
        try {
            circuitBreaker.executeVoid(() -> {
                Set<String> keys = redisTemplate.keys(keyPattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("Cleared {} keys matching pattern: {}", keys.size(), keyPattern);
                }
            });
        } catch (CacheUnavailableException e) {
            log.warn("Cache unavailable (circuit open), skipping CLEAR for pattern: {}", keyPattern);
        } catch (Exception e) {
            log.warn("Cache CLEAR failed for pattern {}: {}", keyPattern, e.getMessage());
        }
    }

    public String getCircuitBreakerState() {
        return circuitBreaker.getState();
    }
}

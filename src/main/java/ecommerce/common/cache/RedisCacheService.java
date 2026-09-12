package ecommerce.common.cache;

import ecommerce.common.cache.exception.CacheUnavailableException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
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
    private final MeterRegistry meterRegistry;

    private Counter hitCounter;
    private Counter missCounter;
    private Counter errorCounter;
    private Counter circuitOpenCounter;
    private Timer getTimer;

    @PostConstruct
    private void initMetrics() {
        hitCounter = Counter.builder("fynza.cache.redis.hits")
                .description("Cache-aside Redis hits")
                .register(meterRegistry);
        missCounter = Counter.builder("fynza.cache.redis.misses")
                .description("Cache-aside Redis misses")
                .register(meterRegistry);
        errorCounter = Counter.builder("fynza.cache.redis.errors")
                .description("Redis operation errors (exceptions)")
                .register(meterRegistry);
        circuitOpenCounter = Counter.builder("fynza.cache.redis.circuit.open")
                .description("Requests rejected because the circuit breaker is OPEN")
                .register(meterRegistry);
        getTimer = Timer.builder("fynza.cache.redis.get.duration")
                .description("Latency of Redis GET operations including fallback path")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return getTimer.record(() -> {
            try {
                return circuitBreaker.execute(() -> {
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value == null) {
                        missCounter.increment();
                        return Optional.<T>empty();
                    }
                    if (type.isInstance(value)) {
                        hitCounter.increment();
                        return Optional.of(type.cast(value));
                    }
                    log.warn("Cache type mismatch for key {}: expected {}, got {}",
                            key, type.getSimpleName(), value.getClass().getSimpleName());
                    missCounter.increment();
                    return Optional.<T>empty();
                });
            } catch (CacheUnavailableException e) {
                circuitOpenCounter.increment();
                log.warn("Cache unavailable (circuit open), miss for key: {}", key);
                return Optional.empty();
            } catch (Exception e) {
                errorCounter.increment();
                log.warn("Cache GET failed for key {}: {}", key, e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            circuitBreaker.executeVoid(() ->
                    redisTemplate.opsForValue().set(key, value, ttl));
        } catch (CacheUnavailableException e) {
            circuitOpenCounter.increment();
            log.warn("Cache unavailable (circuit open), skipping PUT for key: {}", key);
        } catch (Exception e) {
            errorCounter.increment();
            log.warn("Cache PUT failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        try {
            circuitBreaker.executeVoid(() -> redisTemplate.delete(key));
        } catch (CacheUnavailableException e) {
            circuitOpenCounter.increment();
            log.warn("Cache unavailable (circuit open), skipping EVICT for key: {}", key);
        } catch (Exception e) {
            errorCounter.increment();
            log.warn("Cache EVICT failed for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(circuitBreaker.execute(
                    () -> redisTemplate.hasKey(key)));
        } catch (CacheUnavailableException e) {
            circuitOpenCounter.increment();
            log.warn("Cache unavailable (circuit open), exists check fails-open for key: {}", key);
            return false;
        } catch (Exception e) {
            errorCounter.increment();
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
            circuitOpenCounter.increment();
            log.warn("Cache unavailable (circuit open), skipping CLEAR for pattern: {}", keyPattern);
        } catch (Exception e) {
            errorCounter.increment();
            log.warn("Cache CLEAR failed for pattern {}: {}", keyPattern, e.getMessage());
        }
    }

    public String getCircuitBreakerState() {
        return circuitBreaker.getState();
    }
}

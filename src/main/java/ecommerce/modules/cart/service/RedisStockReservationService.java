package ecommerce.modules.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockReservationService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_RESERVATION_KEY_PREFIX = "stock:reserve:";
    private static final String STOCK_LOCK_KEY_PREFIX = "stock:lock:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    public boolean reserveStock(UUID productId, int quantity) {
        String key = STOCK_RESERVATION_KEY_PREFIX + productId;
        String lockKey = STOCK_LOCK_KEY_PREFIX + productId;

        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                Long currentReserved = getReservedQuantity(productId);
                Long availableStock = getAvailableStock(productId);

                if (availableStock == null || availableStock < quantity) {
                    log.warn("Insufficient stock for product {}: available={}, requested={}",
                            productId, availableStock, quantity);
                    return false;
                }

                if (currentReserved == null) {
                    currentReserved = 0L;
                }

                long newReserved = currentReserved + quantity;
                redisTemplate.opsForValue().set(key, newReserved, DEFAULT_TTL);

                log.info("Stock reserved for product {}: quantity={}, total reserved={}",
                        productId, quantity, newReserved);
                return true;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        log.debug("Could not acquire lock for product {}", productId);
        return false;
    }

    public boolean releaseStock(UUID productId, int quantity) {
        String key = STOCK_RESERVATION_KEY_PREFIX + productId;
        String lockKey = STOCK_LOCK_KEY_PREFIX + productId;

        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                Long currentReserved = getReservedQuantity(productId);

                if (currentReserved == null || currentReserved == 0) {
                    log.warn("No reserved stock to release for product {}", productId);
                    return false;
                }

                long newReserved = Math.max(0, currentReserved - quantity);

                if (newReserved > 0) {
                    redisTemplate.opsForValue().set(key, newReserved, DEFAULT_TTL);
                } else {
                    redisTemplate.delete(key);
                }

                log.info("Released stock for product {}: quantity={}, remaining reserved={}",
                        productId, quantity, newReserved);
                return true;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        return false;
    }

    public Long getReservedQuantity(UUID productId) {
        String key = STOCK_RESERVATION_KEY_PREFIX + productId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value != null ? Long.parseLong(value.toString()) : null;
    }

    public boolean hasReservation(UUID productId) {
        String key = STOCK_RESERVATION_KEY_PREFIX + productId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void clearReservation(UUID productId) {
        String key = STOCK_RESERVATION_KEY_PREFIX + productId;
        redisTemplate.delete(key);
        log.info("Cleared stock reservation for product {}", productId);
    }

    public long getTTL(UUID productId) {
        String key = STOCK_RESERVATION_KEY_PREFIX + productId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    private Long getAvailableStock(UUID productId) {
        String key = "stock:available:" + productId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value != null ? Long.parseLong(value.toString()) : null;
    }

    public void setAvailableStock(UUID productId, long quantity) {
        String key = "stock:available:" + productId;
        redisTemplate.opsForValue().set(key, quantity);
        log.debug("Set available stock for product {}: quantity={}", productId, quantity);
    }
}

package ecommerce.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RateLimitingAspect {

    private final Map<String, AtomicLong> rateLimitHits = new ConcurrentHashMap<>();

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        rateLimitHits.forEach((key, count) -> stats.put(key, count.get()));
        return stats;
    }

    public void recordRateLimitHit(String key) {
        rateLimitHits.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}

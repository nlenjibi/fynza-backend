package ecommerce.common.cache;

import ecommerce.common.cache.exception.CacheUnavailableException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class RedisCircuitBreaker {

    private enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration waitDuration;
    private final int halfOpenRequests;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
    private volatile Instant openedAt;

    public RedisCircuitBreaker(int failureThreshold, Duration waitDuration, int halfOpenRequests) {
        this.failureThreshold = failureThreshold;
        this.waitDuration = waitDuration;
        this.halfOpenRequests = halfOpenRequests;
    }

    public boolean allowRequest() {
        State current = state.get();

        if (current == State.CLOSED) {
            return true;
        }

        if (current == State.OPEN) {
            if (Instant.now().isAfter(openedAt.plus(waitDuration))) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenAttempts.set(0);
                    log.info("Circuit breaker transitioning OPEN → HALF_OPEN");
                }
                return state.get() == State.HALF_OPEN;
            }
            return false;
        }

        // HALF_OPEN: allow limited probe requests
        return halfOpenAttempts.get() < halfOpenRequests;
    }

    public void recordSuccess() {
        State current = state.get();
        if (current == State.HALF_OPEN) {
            if (halfOpenAttempts.incrementAndGet() >= halfOpenRequests) {
                state.set(State.CLOSED);
                failureCount.set(0);
                log.info("Circuit breaker transitioning HALF_OPEN → CLOSED (Redis recovered)");
            }
        } else if (current == State.CLOSED) {
            failureCount.set(0);
        }
    }

    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        if (failures >= failureThreshold && state.compareAndSet(State.CLOSED, State.OPEN)) {
            openedAt = Instant.now();
            log.warn("Circuit breaker OPEN after {} consecutive Redis failures", failures);
        } else if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
            openedAt = Instant.now();
            log.warn("Circuit breaker reverting HALF_OPEN → OPEN (probe failed)");
        }
    }

    public boolean isOpen() {
        return state.get() == State.OPEN;
    }

    public String getState() {
        return state.get().name();
    }

    public <T> T execute(java.util.function.Supplier<T> action) {
        if (!allowRequest()) {
            throw new CacheUnavailableException("Redis circuit breaker is OPEN");
        }
        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    public void executeVoid(Runnable action) {
        if (!allowRequest()) {
            throw new CacheUnavailableException("Redis circuit breaker is OPEN");
        }
        try {
            action.run();
            recordSuccess();
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
}

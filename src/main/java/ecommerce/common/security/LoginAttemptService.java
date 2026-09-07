package ecommerce.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service to track failed login attempts and handle account lockout.
 * Uses an in-memory Caffeine cache with a 15-minute expiration.
 * 
 * <p>Per S1.1: Tracks both per-email and per-IP lockout logic.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    // Cache for tracking failed attempts: key = email or IP, value = attempt count
    private final Cache<String, Integer> attemptsCache;

    public LoginAttemptService() {
        this.attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    /**
     * Record a successful login - clears the attempt counter for the given key.
     * @param key The email or IP address that successfully authenticated
     */
    public void loginSucceeded(String key) {
        attemptsCache.invalidate(key);
        log.debug("Login succeeded for key: {}, attempts cleared", maskKey(key));
    }

    /**
     * Record a failed login attempt.
     * @param key The email or IP address that failed to authenticate
     */
    public void loginFailed(String key) {
        int attempts = getAttempts(key);
        attempts++;
        attemptsCache.put(key, attempts);
        if (attempts >= MAX_ATTEMPTS) {
            log.warn("Account locked for key: {} after {} failed attempts", maskKey(key), attempts);
        } else {
            log.debug("Failed login attempt for key: {}, attempts: {}/{}", maskKey(key), attempts, MAX_ATTEMPTS);
        }
    }

    /**
     * Check if the given key is currently locked due to too many failed attempts.
     * @param key The email or IP address to check
     * @return true if locked, false otherwise
     */
    public boolean isLocked(String key) {
        return getAttempts(key) >= MAX_ATTEMPTS;
    }

    /**
     * Get the number of failed attempts for a given key.
     * @param key The email or IP address to check
     * @return Number of failed attempts
     */
    public int getAttempts(String key) {
        Integer attempts = attemptsCache.getIfPresent(key);
        return attempts == null ? 0 : attempts;
    }

    /**
     * Clear all attempt records (useful for testing or admin reset).
     */
    public void clearAll() {
        attemptsCache.invalidateAll();
        log.info("All login attempt records cleared");
    }

    /**
     * Get the remaining lockout time in minutes for a given key.
     * @param key The email or IP address to check
     * @return Remaining minutes, or 0 if not locked
     */
    public int getRemainingLockoutMinutes(String key) {
        if (!isLocked(key)) {
            return 0;
        }
        // Caffeine doesn't expose remaining time directly, 
        // so we return the full duration when locked
        return LOCKOUT_DURATION_MINUTES;
    }

    /**
     * Mask a key for safe logging (shows only first few characters).
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "***";
        }
        return key.substring(0, 2) + "***" + key.substring(key.length() - 2);
    }
}

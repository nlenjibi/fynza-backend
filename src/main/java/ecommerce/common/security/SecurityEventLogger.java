package ecommerce.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventLogger {

    @Async("securityEventExecutor")
    public void logLoginAttempt(String email, String ip, String userAgent, 
                               boolean success, String method, String failureReason) {
        if (success) {
            log.info("LOGIN_SUCCESS: email={}, ip={}, method={}, userAgent={}",
                    email, ip, method, truncateUserAgent(userAgent));
        } else {
            log.warn("LOGIN_FAILURE: email={}, ip={}, method={}, reason={}, userAgent={}",
                    email, ip, method, failureReason, truncateUserAgent(userAgent));
        }
    }

    @Async("securityEventExecutor")
    public void logLogout(UUID userId, String email, String ip) {
        log.info("LOGOUT: userId={}, email={}, ip={}", userId, email, ip);
    }

    @Async("securityEventExecutor")
    public void logTokenRefresh(UUID userId, String email, boolean success) {
        if (success) {
            log.info("TOKEN_REFRESH_SUCCESS: userId={}, email={}", userId, email);
        } else {
            log.warn("TOKEN_REFRESH_FAILURE: userId={}, email={}", userId, email);
        }
    }

    @Async("securityEventExecutor")
    public void logAccountLockout(String email, String ip, int failedAttempts) {
        log.warn("ACCOUNT_LOCKED: email={}, ip={}, failedAttempts={}", email, ip, failedAttempts);
    }

    @Async("securityEventExecutor")
    public void logAccountUnlock(String email) {
        log.info("ACCOUNT_UNLOCKED: email={}", email);
    }

    @Async("securityEventExecutor")
    public void logPasswordChange(UUID userId, String email, boolean success, String reason) {
        if (success) {
            log.info("PASSWORD_CHANGED: userId={}, email={}", userId, email);
        } else {
            log.warn("PASSWORD_CHANGE_FAILED: userId={}, email={}, reason={}", userId, email, reason);
        }
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) return "unknown";
        return userAgent.length() > 100 ? userAgent.substring(0, 100) + "..." : userAgent;
    }
}

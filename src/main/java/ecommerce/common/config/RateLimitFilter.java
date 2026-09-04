package ecommerce.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * IP-based sliding-window rate limiter backed by Redis.
 * Uses a fixed-window counter keyed on the current time slot:
 *   rate_limit:{tier}:{ip}:{slotIndex}
 * where slotIndex = epochSeconds / windowSeconds.
 *
 * Fails open — if Redis is unreachable, requests are passed through.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX      = "rate_limit:";
    private static final String HEADER_LIMIT     = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY     = "Retry-After";

    private final RateLimitProperties props;
    private final StringRedisTemplate  redis;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         chain)
            throws ServletException, IOException {

        if (!props.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String ip   = resolveClientIp(request);
        String path = request.getRequestURI();
        RateLimitProperties.Tier tier = selectTier(path);
        String tierName = isLoginPath(path) ? "login" : "api";

        try {
            long slot     = Instant.now().getEpochSecond() / tier.getWindowSeconds();
            String key    = KEY_PREFIX + tierName + ":" + ip + ":" + slot;
            Long   count  = redis.opsForValue().increment(key);

            if (count == null) count = 1L;
            if (count == 1L) {
                redis.expire(key, tier.getWindowSeconds(), TimeUnit.SECONDS);
            }

            int remaining = Math.max(0, tier.getMaxRequests() - count.intValue());
            response.setIntHeader(HEADER_LIMIT,     tier.getMaxRequests());
            response.setIntHeader(HEADER_REMAINING, remaining);

            if (count > tier.getMaxRequests()) {
                response.setIntHeader(HEADER_RETRY, tier.getWindowSeconds());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Too many requests\",\"retryAfterSeconds\":" + tier.getWindowSeconds() + "}");
                log.warn("Rate limit exceeded: tier={}, ip={}, count={}", tierName, ip, count);
                return;
            }
        } catch (Exception e) {
            log.warn("Rate limiter Redis error — failing open: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private RateLimitProperties.Tier selectTier(String path) {
        return isLoginPath(path) ? props.getLogin() : props.getApi();
    }

    private boolean isLoginPath(String path) {
        return path != null && (path.contains("/auth/login") || path.contains("/auth/callback"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

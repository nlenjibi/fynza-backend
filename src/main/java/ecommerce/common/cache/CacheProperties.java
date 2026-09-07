package ecommerce.common.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("fynza.cache")
public class CacheProperties {

    private Redis redis = new Redis();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Getter
    @Setter
    public static class Redis {
        private Duration defaultTtl = Duration.ofMinutes(15);
        private Duration tokenBlacklistTtl = Duration.ofHours(24);
        private Duration userTokenVersionTtl = Duration.ofHours(24);
    }

    @Getter
    @Setter
    public static class CircuitBreaker {
        private int failureThreshold = 5;
        private Duration waitDuration = Duration.ofSeconds(30);
        private int halfOpenRequests = 3;
    }
}

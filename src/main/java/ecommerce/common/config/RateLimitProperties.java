package ecommerce.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Login endpoint tier — stricter to defend against credential stuffing. */
    private Tier login = new Tier(5, 60);

    /** General API tier for all other authenticated/unauthenticated endpoints. */
    private Tier api = new Tier(100, 60);

    @Data
    public static class Tier {
        private int maxRequests;
        private int windowSeconds;

        public Tier(int maxRequests, int windowSeconds) {
            this.maxRequests   = maxRequests;
            this.windowSeconds = windowSeconds;
        }

        public Tier() {}
    }
}

package ecommerce.modules.payout.provider;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "fynza.payout")
@Getter
@Setter
public class PayoutProperties {

    private String provider = "paystack";
    private int settlementDays = 3;
    private BigDecimal minimumAmount = new BigDecimal("20.00");
    private int maxRetryCount = 3;
    private PaystackPayoutConfig paystack = new PaystackPayoutConfig();

    @Getter
    @Setter
    public static class PaystackPayoutConfig {
        private String secretKey = "";
        private String webhookSecret = "";
        private String baseUrl = "https://api.paystack.co";
        private int connectTimeout = 30_000;
        private int readTimeout = 30_000;
    }
}

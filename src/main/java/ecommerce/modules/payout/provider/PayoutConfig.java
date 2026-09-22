package ecommerce.modules.payout.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class PayoutConfig {

    private final PayoutProperties payoutProperties;

    @Bean("paystackPayoutRestTemplate")
    @ConditionalOnProperty(name = "fynza.payout.provider", havingValue = "paystack", matchIfMissing = true)
    public RestTemplate paystackPayoutRestTemplate() {
        PayoutProperties.PaystackPayoutConfig cfg = payoutProperties.getPaystack();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(cfg.getConnectTimeout());
        factory.setReadTimeout(cfg.getReadTimeout());
        return new RestTemplate(factory);
    }
}

package ecommerce.modules.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class PaystackConfig {

    private final PaymentProperties paymentProperties;

    @Bean
    @ConditionalOnProperty(name = "fynza.payment.provider", havingValue = "paystack", matchIfMissing = true)
    public RestTemplate paystackRestTemplate() {
        PaymentProperties.PaystackConfig cfg = paymentProperties.getPaystack();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(cfg.getConnectTimeout());
        factory.setReadTimeout(cfg.getReadTimeout());
        return new RestTemplate(factory);
    }
}

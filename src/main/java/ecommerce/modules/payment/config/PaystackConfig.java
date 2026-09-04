package ecommerce.modules.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Paystack payment gateway configuration.
 */
@Configuration
@RequiredArgsConstructor
public class PaystackConfig {

    private final PaystackProperties paystackProperties;

    @Bean
    public RestTemplate paystackRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(paystackProperties.getConnectTimeout());
        factory.setReadTimeout(paystackProperties.getReadTimeout());
        return new RestTemplate(factory);
    }
}
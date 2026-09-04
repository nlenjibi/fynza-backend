package ecommerce.modules.payment;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class PaymentSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Paystack payment endpoints - public access for payment processing
                .requestMatchers(HttpMethod.POST, "/v1/payments/paystack/initialize").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/payments/paystack/verify/{reference}").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/payments/paystack/refund/{reference}").permitAll()
                
                // Paystack webhook endpoint - public access for external payment gateway
                .requestMatchers(HttpMethod.POST, "/v1/webhooks/paystack").permitAll();
    }
}

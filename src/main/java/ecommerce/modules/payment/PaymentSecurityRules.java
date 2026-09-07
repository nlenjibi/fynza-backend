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
                // Initialize and refund require an authenticated customer
                .requestMatchers(HttpMethod.POST, "/v1/payments/paystack/initialize").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/v1/payments/paystack/refund/{reference}").hasRole("CUSTOMER")

                // Verify is public so redirect callbacks from Paystack work without a session
                .requestMatchers(HttpMethod.GET, "/v1/payments/paystack/verify/{reference}").permitAll()

                // Webhook endpoint is public — Paystack calls it without a JWT; signature verified in handler
                .requestMatchers(HttpMethod.POST, "/v1/webhooks/paystack").permitAll();
    }
}

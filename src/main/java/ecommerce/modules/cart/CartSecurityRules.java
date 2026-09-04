package ecommerce.modules.cart;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CartSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // All cart endpoints require CUSTOMER role (class-level @PreAuthorize)
                .requestMatchers( "/v1/cart").hasRole("CUSTOMER")
                .requestMatchers( "/v1/cart/reservations/{reservationId}").hasRole("CUSTOMER")
                .requestMatchers( "/v1/cart/**").hasRole("CUSTOMER");
    }
}

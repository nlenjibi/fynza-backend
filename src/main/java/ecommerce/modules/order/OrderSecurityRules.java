package ecommerce.modules.order;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class OrderSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Checkout — authenticated customers with order:write permission
                .requestMatchers(HttpMethod.POST, "/v1/checkout").hasAuthority("order:write")

                // Customer order mutations
                .requestMatchers(HttpMethod.POST, "/v1/orders/{id}/cancel").hasAuthority("order:write")

                // Seller order mutations
                .requestMatchers(HttpMethod.PATCH, "/v1/seller/orders/{id}/status").hasAuthority("order:write")

                // Admin order mutations
                .requestMatchers(HttpMethod.PATCH, "/v1/admin/orders/{id}").hasAuthority("order:admin");
    }
}

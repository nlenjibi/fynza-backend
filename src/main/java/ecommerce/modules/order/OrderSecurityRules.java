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
                // Checkout — authenticated customers only
                .requestMatchers(HttpMethod.POST, "/v1/checkout").hasRole("CUSTOMER")

                // Customer order queries and cancellation
                .requestMatchers(HttpMethod.GET, "/v1/customers/orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/v1/customers/orders/{id}").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/v1/customers/orders/{id}/cancel").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/v1/customers/orders/{id}/refund").hasRole("CUSTOMER")

                // Order tracking — customers, sellers and admins; secured at method level per resolver
                .requestMatchers(HttpMethod.GET, "/v1/orders/{orderId}/tracking").authenticated()
                .requestMatchers(HttpMethod.GET, "/v1/orders/{orderId}/timeline").authenticated()

                // Admin order management
                .requestMatchers(HttpMethod.GET, "/v1/admin/orders/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/v1/admin/orders/{id}").hasAnyRole("ADMIN", "MANAGER");
    }
}

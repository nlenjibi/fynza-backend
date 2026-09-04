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
                // Customer order endpoints - all require CUSTOMER role
                .requestMatchers(HttpMethod.GET, "/v1/customers/orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/v1/customers/orders/{id}").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/v1/customers/orders/{id}/cancel").hasRole("CUSTOMER");
    }
}

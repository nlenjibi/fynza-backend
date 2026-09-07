package ecommerce.modules.user;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class UserSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Admin user management
                .requestMatchers("/v1/users/**").hasRole("ADMIN")

                // Customer self-service endpoints
                .requestMatchers(HttpMethod.GET,    "/v1/customers/profile").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT,    "/v1/customers/profile").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET,    "/v1/customers/dashboard").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET,    "/v1/customers/loyalty/balance").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST,   "/v1/customers/loyalty/redeem").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET,    "/v1/customers/addresses").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST,   "/v1/customers/addresses").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT,    "/v1/customers/addresses/{id}").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/v1/customers/addresses/{id}").hasRole("CUSTOMER");
    }
}

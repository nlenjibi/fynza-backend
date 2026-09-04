package ecommerce.modules.message;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class MessageSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Admin message management endpoints - all require ADMIN role
                .requestMatchers( "/v1/admin/**").hasRole("ADMIN")

                // Customer message management endpoints - all require CUSTOMER role
                .requestMatchers( "/v1/customer/**").hasRole("CUSTOMER")

                .requestMatchers( "/v1/sellers/**").hasRole("SELLER");
    }
}

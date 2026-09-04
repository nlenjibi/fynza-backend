package ecommerce.modules.refund;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class RefundSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Admin refund management endpoints - all require ADMIN role
                .requestMatchers( "/v1/admin/**").hasRole("ADMIN")
                
                // Customer refund endpoints - all require CUSTOMER role
                .requestMatchers( "/v1/customer/**").hasRole("CUSTOMER");

    }
}

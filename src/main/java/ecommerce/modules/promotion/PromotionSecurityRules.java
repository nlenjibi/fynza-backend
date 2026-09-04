package ecommerce.modules.promotion;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class PromotionSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Admin promotion management endpoints - all require ADMIN role
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                // Seller promotion management endpoints - all require SELLER role
                .requestMatchers( "/v1/sellers/**").hasRole("SELLER");

    }
}

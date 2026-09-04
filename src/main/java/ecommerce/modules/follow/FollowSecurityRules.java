package ecommerce.modules.follow;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class FollowSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Customer following endpoints - all require CUSTOMER role
                .requestMatchers(HttpMethod.GET, "/v1/customer/follows").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/v1/customer/follows").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/v1/customer/follows/{sellerId}").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/v1/customer/follows/check/{sellerId}").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/v1/customer/follows/stats").hasRole("CUSTOMER")
                
                // Seller follower management endpoints - all require SELLER role
                .requestMatchers(HttpMethod.GET, "/v1/sellers/followers").hasRole("SELLER")
                .requestMatchers(HttpMethod.GET, "/v1/sellers/followers/stats").hasRole("SELLER");
    }
}

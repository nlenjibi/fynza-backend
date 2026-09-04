package ecommerce.modules.coupon;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CouponSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Admin coupon management endpoints
                .requestMatchers(HttpMethod.GET, "/v1/coupons").hasRole("ADMIN")
                .requestMatchers( "/v1/coupons/**").hasRole("ADMIN")
                // Coupon validation - authenticated users
                .requestMatchers(HttpMethod.GET, "/v1/coupons/validate").authenticated()
                
                // Seller coupon management - SELLER role (class-level @PreAuthorize)
                .requestMatchers( "/v1/seller/**").hasRole("SELLER");

    }
}

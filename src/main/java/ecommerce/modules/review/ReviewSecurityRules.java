package ecommerce.modules.review;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ReviewSecurityRules implements SecurityRules {
    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Public read endpoints
                .requestMatchers(HttpMethod.GET, "/v1/reviews/{reviewId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/reviews/product/{productId}").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/reviews/product/{productId}/filter").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/reviews/product/{productId}/stats").permitAll()
                .requestMatchers(HttpMethod.PUT, "/v1/reviews/{reviewId}/helpful").permitAll()
                
                // Admin review management endpoints
                .requestMatchers(HttpMethod.GET, "/v1/reviews/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/reviews/admin/search").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/v1/reviews/{reviewId}/admin-response").hasRole("ADMIN")
                .requestMatchers( "/v1/reviews/admin/**").hasRole("ADMIN")

                
                // Authenticated user endpoints (service enforces ownership)
                .requestMatchers(HttpMethod.DELETE, "/v1/reviews/{reviewId}").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/v1/reviews/{reviewId}/restore").hasRole("CUSTOMER");
    }
}

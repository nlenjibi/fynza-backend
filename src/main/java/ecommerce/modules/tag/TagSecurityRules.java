package ecommerce.modules.tag;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class TagSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Public tag endpoints
                .requestMatchers(HttpMethod.GET, "/v1/tags").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/tags/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/tags/featured").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/tags/popular").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/tags/{id}").permitAll()

                // Admin tag management endpoints
                .requestMatchers(HttpMethod.POST, "/v1/tags").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/tags/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/tags/{id}").hasRole("ADMIN")

                // Seller tag management endpoints - all require SELLER role
                .requestMatchers( "/v1/seller/**").hasRole("SELLER");

    }
}

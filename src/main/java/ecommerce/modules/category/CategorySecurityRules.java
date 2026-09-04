package ecommerce.modules.category;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CategorySecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry

                // GET /stats - ADMIN only
                .requestMatchers(HttpMethod.GET, "/v1/categories/stats").hasRole("ADMIN")

                // POST - SELLER or ADMIN
                .requestMatchers(HttpMethod.POST, "/v1/categories").hasAnyRole("SELLER", "ADMIN")

                // PUT - SELLER or ADMIN
                .requestMatchers(HttpMethod.PUT, "/v1/categories/{id}").hasAnyRole("SELLER", "ADMIN")

                // DELETE - ADMIN only
                .requestMatchers(HttpMethod.DELETE, "/v1/categories/{id}").hasRole("ADMIN")

                // PATCH status - ADMIN only
                .requestMatchers(HttpMethod.PATCH, "/v1/categories/{id}/status").hasRole("ADMIN")
                // GET endpoints - public access
                .requestMatchers(HttpMethod.GET, "/v1/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/categories/tree").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/categories/{id}").permitAll();

    }
}

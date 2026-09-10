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
                // Admin category mutations
                .requestMatchers(HttpMethod.POST,   "/v1/admin/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/v1/admin/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/admin/categories/**").hasRole("ADMIN")

                // Seller suggestion mutation
                .requestMatchers(HttpMethod.POST, "/v1/sellers/me/categories/suggestions")
                    .hasAnyAuthority("category.suggestion.create", "ROLE_ADMIN");
    }
}

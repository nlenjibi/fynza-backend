package ecommerce.modules.contact;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ContactSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/v1/contact").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/contact/{id}/status").permitAll()
                
                // Admin-only endpoints
                .requestMatchers(HttpMethod.GET, "/v1/contact").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contact/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contact/{id}/respond").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contact/{id}/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contact/{id}/assign").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contact/{id}/categorize").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/contact/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contact/search").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contact/my-assigned").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contact/unassigned").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contact/stats").hasRole("ADMIN");
    }
}

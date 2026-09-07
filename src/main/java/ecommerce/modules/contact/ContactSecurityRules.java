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
                .requestMatchers(HttpMethod.POST, "/v1/contacts").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/contacts/{id}/status").permitAll()
                
                // Admin-only endpoints
                .requestMatchers(HttpMethod.GET, "/v1/contacts").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contacts/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contacts/{id}/respond").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contacts/{id}/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contacts/{id}/assign").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/contacts/{id}/categorize").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/contacts/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contacts/search").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contacts/my-assigned").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contacts/unassigned").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/contacts/stats").hasRole("ADMIN");
    }
}

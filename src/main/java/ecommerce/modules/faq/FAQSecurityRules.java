package ecommerce.modules.faq;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class FAQSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Public FAQ endpoints
                .requestMatchers(HttpMethod.GET, "/v1/faqs").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/faqs/**").permitAll()

                // Contact support endpoints
                .requestMatchers(HttpMethod.GET, "/v1/contact/options").permitAll()
                
                // Help endpoints - mixed access
                .requestMatchers(HttpMethod.GET, "/v1/help/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/customer/help/contact").authenticated()
                .requestMatchers(HttpMethod.POST, "/v1/help/contact").permitAll()
                
                // Admin FAQ management endpoints
                .requestMatchers( "/v1/admin/**").hasRole("ADMIN");

    }
}

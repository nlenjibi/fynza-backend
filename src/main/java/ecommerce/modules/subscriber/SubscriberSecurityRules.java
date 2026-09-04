package ecommerce.modules.subscriber;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class SubscriberSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Public subscriber endpoints
                .requestMatchers(HttpMethod.POST, "/v1/subscribers").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/subscribers/unsubscribe/{id}").permitAll()
                
                // Admin subscriber management endpoints
                .requestMatchers("/v1/admin/**").hasRole("ADMIN");

    }
}

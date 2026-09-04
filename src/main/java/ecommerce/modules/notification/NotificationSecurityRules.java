package ecommerce.modules.notification;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class NotificationSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Notification endpoints - all require authentication
                .requestMatchers(HttpMethod.GET, "/v1/notifications").authenticated()
                .requestMatchers(HttpMethod.PUT, "/v1/notifications/{id}/read").authenticated()
                .requestMatchers(HttpMethod.PUT, "/v1/notifications/read-all").authenticated();
    }
}

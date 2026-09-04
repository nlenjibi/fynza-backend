package ecommerce.modules.delivery;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class DeliverySecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Admin region management endpoints
                .requestMatchers(HttpMethod.POST, "/v1/admin/delivery/regions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/admin/delivery/regions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/admin/delivery/regions/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/admin/delivery/regions/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/admin/delivery/regions/{id}").hasRole("ADMIN")
                
                // Admin delivery fee management endpoints
                .requestMatchers(HttpMethod.POST, "/v1/admin/delivery/fees").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/admin/delivery/fees").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/admin/delivery/fees/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/admin/delivery/fees/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/admin/delivery/fees/{id}").hasRole("ADMIN")
                
                // Public delivery information endpoints
                .requestMatchers(HttpMethod.GET, "/v1/delivery/regions").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/delivery/fees").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/delivery/fees/region/{regionId}").permitAll();
    }
}

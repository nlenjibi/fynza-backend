package ecommerce.modules.product;

import ecommerce.common.security.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ProductSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/v1/products", "/v1/products/").permitAll()
                .requestMatchers("/v1/products/{id}").permitAll()
                .requestMatchers("/v1/products/{id}/view").permitAll()
                .requestMatchers("/v1/products/{id}/rating").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/v1/products").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/v1/products/{id}").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/v1/products/{id}").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/v1/products/seller/products").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/v1/products/{id}/stock/**").hasAnyRole("SELLER", "ADMIN");
    }
}

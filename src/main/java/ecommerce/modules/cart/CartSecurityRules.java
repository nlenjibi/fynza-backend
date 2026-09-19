package ecommerce.modules.cart;

import ecommerce.common.security.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CartSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/v1/cart/guest").permitAll()
                .requestMatchers("/v1/cart/guest/**/items").permitAll()
                .requestMatchers("/v1/cart/validate").permitAll()
                .requestMatchers("/v1/cart/**").hasAuthority("cart:read");
    }
}

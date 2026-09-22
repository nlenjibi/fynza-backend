package ecommerce.modules.shipping;

import ecommerce.common.security.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ShippingSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Public: tracking lookup and rate calculation
                .requestMatchers("POST", "/v1/shipping/rates").permitAll()
                // Seller shipment management
                .requestMatchers("/v1/seller/shipping/**").hasAuthority("shipping:write")
                // Admin management
                .requestMatchers("/v1/admin/shipping/**").hasAuthority("shipping:admin")
                // Webhooks from carriers — must be open for carrier callbacks
                .requestMatchers("/v1/shipping/webhooks/**").permitAll();
    }
}

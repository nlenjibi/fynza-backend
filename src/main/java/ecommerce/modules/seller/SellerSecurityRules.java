package ecommerce.modules.seller;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class SellerSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // ── Seller self-service ───────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/v1/sellers/me").hasRole("SELLER")
                .requestMatchers(HttpMethod.PATCH, "/v1/sellers/me").hasRole("SELLER")
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/onboarding").hasRole("SELLER")
                .requestMatchers(HttpMethod.PATCH, "/v1/sellers/me/onboarding").hasRole("SELLER")
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/application").hasRole("SELLER")
                .requestMatchers(HttpMethod.GET,   "/v1/sellers/me/verification").hasRole("SELLER")
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/verification").hasRole("SELLER")

                // ── Admin seller management ───────────────────────────────────────────
                .requestMatchers("/v1/admin/sellers/**").hasRole("ADMIN");
    }
}

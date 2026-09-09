package ecommerce.modules.customer;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CustomerSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // ── Customer self-service mutations (reads go via GraphQL /graphql) ────
                .requestMatchers(HttpMethod.PATCH,  "/v1/customers/me").hasAuthority("customer.update.own")
                .requestMatchers(HttpMethod.PATCH,  "/v1/customers/me/preferences").hasAuthority("customer.update.own")
                .requestMatchers(HttpMethod.POST,   "/v1/customers/me/addresses").hasAuthority("address.create.own")
                .requestMatchers(HttpMethod.PATCH,  "/v1/customers/me/addresses/**").hasAuthority("address.update.own")
                .requestMatchers(HttpMethod.DELETE, "/v1/customers/me/addresses/**").hasAuthority("address.delete.own")
                .requestMatchers(HttpMethod.POST,   "/v1/customers/me/addresses/**").hasAuthority("address.update.own")

                // ── Admin customer mutations ───────────────────────────────────────────
                .requestMatchers(HttpMethod.PATCH, "/v1/admin/customers/**").hasAnyAuthority(
                        "customer.manage", "customer.suspend", "customer.activate", "customer.block");
    }
}

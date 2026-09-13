package ecommerce.modules.store;

import ecommerce.common.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class StoreSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // ── Public store browsing ─────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/v1/stores").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/stores/{slug}").permitAll()

                // ── Seller self-service ───────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/stores").hasAuthority("store.create.own")
                .requestMatchers(HttpMethod.GET,   "/v1/sellers/me/stores/me").hasAuthority("store.read.own")
                .requestMatchers(HttpMethod.PATCH,  "/v1/sellers/me/stores/me").hasAuthority("store.update.own")
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/stores/me/submit").hasAuthority("store.publish.own")
                .requestMatchers(HttpMethod.PATCH, "/v1/sellers/me/stores/me/visibility").hasAuthority("store.publish.own")
                .requestMatchers(HttpMethod.GET,   "/v1/sellers/me/stores/me/settings").hasAuthority("store.manage.own")
                .requestMatchers(HttpMethod.PATCH, "/v1/sellers/me/stores/me/settings").hasAuthority("store.manage.own")
                .requestMatchers("/v1/sellers/me/stores/me/policies/**").hasAuthority("store.manage.own")
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/stores/me/pause").hasAuthority("store.pause.own")
                .requestMatchers(HttpMethod.POST,  "/v1/sellers/me/stores/me/close").hasAuthority("store.delete.own")

                // ── Admin store management ────────────────────────────────────────────
                .requestMatchers("/v1/admin/stores/**").hasRole("ADMIN");
    }
}

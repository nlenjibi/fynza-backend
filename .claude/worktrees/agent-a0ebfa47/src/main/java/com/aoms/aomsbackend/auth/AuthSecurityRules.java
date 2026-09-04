package com.aoms.aomsbackend.auth;

import com.aoms.aomsbackend.config.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class AuthSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Truly public — no token needed at all
                .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/callback",
                        "/api/v2/auth/login",
                        "/api/v2/auth/logout"
                ).permitAll()
                // Session-based endpoints — accessible to any authenticated user
                // These require SESSION cookie from login
                .requestMatchers(
                        "/api/v1/auth/me",
                        "/api/v1/auth/validate",
                        "/api/v2/auth/me",
                        "/api/v2/auth/validate"
                ).authenticated();
    }
}

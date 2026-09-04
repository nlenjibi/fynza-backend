package com.aoms.aomsbackend.attendance;

import com.aoms.aomsbackend.config.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Spring Security rules for the attendance module.
 * Requires authentication for all {@code /api/v1/attendance/**} endpoints.
 * Fine-grained role checks are handled by the {@code @RequiresRole} interceptor.
 */
@Component
public class AttendanceSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/api/v1/attendance/**").authenticated()
                .requestMatchers("/api/v1/locations/**").authenticated();
    }
}

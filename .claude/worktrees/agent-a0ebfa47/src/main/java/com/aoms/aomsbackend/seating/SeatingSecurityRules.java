package com.aoms.aomsbackend.seating;

import com.aoms.aomsbackend.config.SecurityRules;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class SeatingSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers("/api/v1/buildings/*/floors/**").authenticated();
        registry.requestMatchers("/api/v1/locations/*/floor-plan").authenticated();
        registry.requestMatchers("/api/v1/seat-bookings", "/api/v1/seat-bookings/**").authenticated();

    }
}

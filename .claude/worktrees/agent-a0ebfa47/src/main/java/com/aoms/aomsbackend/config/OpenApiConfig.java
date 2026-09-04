package com.aoms.aomsbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AOMS Backend API")
                        .description("API documentation for the AOMS backend application")
                        .version("v1"))
                .components(new Components()
                        // Session cookie authentication scheme
                        .addSecuritySchemes("session_cookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("SESSION")
                                .description("HttpOnly session cookie. Automatically set by /login endpoints.")));
    }
}


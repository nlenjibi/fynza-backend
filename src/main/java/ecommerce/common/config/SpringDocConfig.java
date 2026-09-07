package ecommerce.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfig {

    @Value("${server.port:9190}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${app.base-url:https://api.fynza.com}")
    private String productionBaseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("Authorization")
                .description("JWT access token. Obtain from POST /api/v1/auth/login");

        SecurityScheme oauth2Scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl("https://accounts.google.com/o/oauth2/v2/auth")
                                .tokenUrl("https://oauth2.googleapis.com/token")
                                .scopes(new Scopes()
                                        .addString("email",   "Access email address")
                                        .addString("profile", "Access profile info"))));

        return new OpenAPI()
                .info(new Info()
                        .title("Fynza E-Commerce API")
                        .version("v1.0.0")
                        .description("""
                                ## Authentication
                                - **JWT**: Click **Authorize**, enter your Bearer token
                                - **OAuth2**: Use the Google OAuth2 flow via `/api/v1/auth/oauth2/google`
                                """)
                        .contact(new Contact().name("Fynza Team").email("dev@fynza.com"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + contextPath)
                                .description("Local"),
                        new Server()
                                .url(productionBaseUrl)
                                .description("Production")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",    jwtScheme)
                        .addSecuritySchemes("Google-OAuth2", oauth2Scheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch(
                        "/api/v1/auth/**",
                        "/api/v1/products/**",
                        "/api/v1/categories/**",
                        "/api/v1/search/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .pathsToMatch(
                        "/api/v1/user/**",
                        "/api/v1/orders/**",
                        "/api/v1/cart/**",
                        "/api/v1/wishlist/**",
                        "/api/v1/reviews/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi sellerApi() {
        return GroupedOpenApi.builder()
                .group("seller")
                .pathsToMatch("/api/v1/seller/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch(
                        "/api/v1/admin/**",
                        "/api/v1/analytics/**",
                        "/api/v1/search/analytics",
                        "/api/v1/audit-logs/**",
                        "/api/v1/monitoring/**"
                )
                .build();
    }
}

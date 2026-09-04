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

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("Authorization")
                .description("Enter your JWT token. Click 'Authorize' button to login.");

        Scopes oauth2Scopes = new Scopes();
        oauth2Scopes.put("email", "Access to email");
        oauth2Scopes.put("profile", "Access to profile");
        SecurityScheme oauth2SecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl("https://accounts.google.com/o/oauth2/v2/auth")
                                .tokenUrl("https://oauth2.googleapis.com/token")
                                .scopes(oauth2Scopes)));

        SecurityRequirement jwtRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Smart E-Commerce System API")
                        .version("v1.0.0")
                        .description("""
                                ## Smart E-Commerce API - Security Documentation
                                
                                ### How to Use
                                1. Click **Authorize** button below
                                2. Enter your JWT token (format: `Bearer {token}`)
                                3. Click **Authorize** to apply to all requests
                                4. Test protected endpoints - they will return 401 if not authenticated
                                """)
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + contextPath)
                                .description("Development Server"),
                        new Server()
                                .url("https://your-production-domain.com" + contextPath)
                                .description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", jwtSecurityScheme)
                        .addSecuritySchemes("Google-OAuth2", oauth2SecurityScheme))
                .addSecurityItem(jwtRequirement);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch(
                        "/v1/auth/login",
                        "/v1/auth/register",
                        "/v1/auth/refresh",
                        "/v1/auth/oauth2/**",
                        "/v1/auth/password/**",
                        "/v1/products/**",
                        "/v1/categories/**",
                        "/public/**",
                        "/info",
                        "/help-support/**",
                        "/social-links",
                        "/app-download-links",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/public",
                        "/graphiql/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .pathsToMatch(
                        "/v1/user/**",
                        "/v1/order/**",
                        "/v1/cart/**",
                        "/v1/wishlist/**",
                        "/v1/review/**"
                )
                .addOpenApiMethodFilter(method ->
                        method.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class))
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch(
                        "/v1/admin/**",
                        "/v1/auth/account/**",
                        "/performance/**",
                        "/management/**"
                )
                .addOpenApiMethodFilter(method ->
                        method.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class))
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/v3/api-docs/all")
                .build();
    }
}

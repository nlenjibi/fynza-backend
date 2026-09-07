package ecommerce.common.config;

import ecommerce.common.exception.CustomAccessDeniedHandler;
import ecommerce.common.exception.CustomAuthenticationEntryPoint;
import ecommerce.common.security.*;
import ecommerce.common.util.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <h3>Session policy</h3>
 * {@code IF_REQUIRED} (not STATELESS) because the OAuth2 authorization-code flow
 * needs a short-lived HTTP session to carry PKCE state between the redirect and the
 * callback. After the success handler sets JWT cookies, the session is discarded.
 *
 * <h3>CSRF</h3>
 * Disabled: REST API consumed by SPA/mobile clients over HTTPS. Origin is strictly
 * restricted by the CORS allowlist, making CSRF attacks infeasible for credentialed
 * cross-origin requests. OWASP A01 is mitigated by the explicit origin allowlist.
 *
 * <h3>CORS</h3>
 * Fully driven by {@code cors.*} properties. Wildcard headers are not used —
 * an explicit allowlist prevents header-injection attacks.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private static final String CSP_POLICY =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://esm.sh; " +
            "style-src 'self' 'unsafe-inline' https://esm.sh; " +
            "img-src 'self' data: blob:; " +
            "font-src 'self' data: https://esm.sh; " +
            "connect-src 'self' https://esm.sh; " +
            "worker-src blob:; " +
            "frame-ancestors 'none'; " +
            "form-action 'self'; " +
            "base-uri 'self'; " +
            "object-src 'none'";

    // ── Core security components ───────────────────────────────────────────────
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    // ── Exception handlers ─────────────────────────────────────────────────────
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    // ── OAuth2 handlers ────────────────────────────────────────────────────────
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    // ── Authorization rules — each module registers its own SecurityRules bean ─
    private final List<SecurityRules> securityRules;

    // ── CORS ───────────────────────────────────────────────────────────────────
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    @Bean
    @SuppressWarnings("java:S4502") // CSRF disabled intentionally — see class Javadoc
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── HTTP security response headers (OWASP A05) ────────────────
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))  // 1 year
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(ct -> {}))         // X-Content-Type-Options: nosniff

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))

                // IF_REQUIRED so OAuth2 state survives the provider redirect
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> {
                    // Public product browsing
                    auth.requestMatchers("/v1/products", "/v1/products/**").permitAll();

                    // Module-scoped rules (Swagger, Auth, etc.)
                    if (securityRules != null) {
                        securityRules.forEach(rule -> rule.configure(auth));
                    }
                    auth.anyRequest().authenticated();
                })

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(httpSessionOAuth2AuthorizationRequestRepository()))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler))

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository httpSessionOAuth2AuthorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        // Explicit allowlist — never use "*" with credentialed requests (OWASP A01)
        config.setAllowedHeaders(List.of(
                "Content-Type", "Authorization", "Accept",
                "X-Requested-With", "Origin", "X-Request-Id", "Idempotency-Key"));
        config.setExposedHeaders(List.of("Authorization", "X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);
        log.debug("CORS configured — allowed origins: {}", allowedOrigins);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

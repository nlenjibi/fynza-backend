package ecommerce.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.config.RateLimitFilter;
import ecommerce.common.config.RateLimitProperties;
import ecommerce.common.exception.CustomAccessDeniedHandler;
import ecommerce.common.exception.CustomAuthenticationEntryPoint;
import ecommerce.common.security.JwtAuthenticationFilter;
import ecommerce.common.security.OAuth2AuthenticationFailureHandler;
import ecommerce.common.security.OAuth2AuthenticationSuccessHandler;
import ecommerce.common.security.UserPrincipal;
import ecommerce.common.util.CustomUserDetailsService;
import ecommerce.modules.user.dto.AccountDeactivationRequest;
import ecommerce.modules.user.dto.AccountDeletionRequest;
import ecommerce.modules.user.dto.UpdateProfileRequest;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ecommerce.modules.user.UserSecurityRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests for {@link MeController} using {@code @WebMvcTest} + MockMvc.
 *
 * <p>Security is enforced via {@code @WithMockUser} / {@code SecurityMockMvcRequestPostProcessors.user()}
 * to inject an authenticated principal. Each test group also covers the unauthenticated
 * (401) path where relevant.
 */
@WebMvcTest(MeController.class)
@ActiveProfiles("test")
@DisplayName("MeController — /v1/users/me")
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserService userService;

    // SecurityConfig dependencies — required for the @WebMvcTest slice to load
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler customAccessDeniedHandler;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private RateLimitFilter rateLimitFilter;
    @MockBean private RateLimitProperties rateLimitProperties;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UUID principalId;
    private UserPrincipal mockPrincipal;
    private UserProfileResponse sampleProfile;

    @BeforeEach
    void setUp() throws Exception {
        principalId = UUID.randomUUID();

        // Build a minimal UserPrincipal without touching the real User entity by
        // using a Mockito spy so we control getId() / getUsername() without JPA.
        mockPrincipal = org.mockito.Mockito.mock(UserPrincipal.class);
        when(mockPrincipal.getId()).thenReturn(principalId);
        when(mockPrincipal.getUsername()).thenReturn("alice@example.com");
        when(mockPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(authorities).when(mockPrincipal).getAuthorities();
        when(mockPrincipal.isEnabled()).thenReturn(true);
        when(mockPrincipal.isAccountNonExpired()).thenReturn(true);
        when(mockPrincipal.isAccountNonLocked()).thenReturn(true);
        when(mockPrincipal.isCredentialsNonExpired()).thenReturn(true);

        sampleProfile = UserProfileResponse.builder()
                .id(principalId)
                .email("alice@example.com")
                .username("alice")
                .firstName("Alice")
                .lastName("Smith")
                .displayName("Ali")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mfaEnabled(false)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2025-06-01T00:00:00Z"))
                .build();

        // JwtAuthenticationFilter and RateLimitFilter extend OncePerRequestFilter whose
        // doFilter() is final. The CGLIB mock cannot override it, so the real doFilter()
        // runs and calls the mocked doFilterInternal() which does nothing. Use anonymous
        // Answer classes (not lambdas) so javac accepts the `throws Throwable` clause.
        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                FilterChain chain = inv.getArgument(2);
                chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
                return null;
            }
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                FilterChain chain = inv.getArgument(2);
                chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
                return null;
            }
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        // Security exception handlers — use sendError() (commits response) to prevent
        // Spring Security from layering a redirect on top of a setStatus() call.
        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                ((HttpServletResponse) inv.getArgument(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }
        }).when(customAuthenticationEntryPoint).commence(any(), any(), any());
        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                ((HttpServletResponse) inv.getArgument(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
        }).when(customAccessDeniedHandler).handle(any(), any(), any());
    }

    // =========================================================================
    // GET /v1/users/me
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/users/me")
    class GetMyProfile {

        @Test
        @DisplayName("200 — authenticated user receives their profile in $.data")
        void whenAuthenticated_returns200WithProfile() throws Exception {
            when(userService.getMyProfile(principalId)).thenReturn(sampleProfile);

            mockMvc.perform(get("/v1/users/me")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(principalId.toString()))
                    .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                    .andExpect(jsonPath("$.data.username").value("alice"))
                    .andExpect(jsonPath("$.data.firstName").value("Alice"))
                    .andExpect(jsonPath("$.data.lastName").value("Smith"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.emailVerified").value(true))
                    .andExpect(jsonPath("$.data.mfaEnabled").value(false));

            verify(userService).getMyProfile(principalId);
        }

        @Test
        @DisplayName("200 — message is 'Profile retrieved'")
        void whenAuthenticated_messageIsProfileRetrieved() throws Exception {
            when(userService.getMyProfile(principalId)).thenReturn(sampleProfile);

            mockMvc.perform(get("/v1/users/me")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profile retrieved"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 — optional profile fields are serialized when present")
        void whenProfileHasOptionalFields_theyAppearInData() throws Exception {
            UserProfileResponse richProfile = UserProfileResponse.builder()
                    .id(principalId)
                    .email("alice@example.com")
                    .username("alice")
                    .firstName("Alice")
                    .lastName("Smith")
                    .displayName("Ali")
                    .phone("+233201234567")
                    .avatarUrl("https://cdn.example.com/avatar.png")
                    .language("en")
                    .timezone("Africa/Accra")
                    .currency("GHS")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .mfaEnabled(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T00:00:00Z"))
                    .build();

            when(userService.getMyProfile(principalId)).thenReturn(richProfile);

            mockMvc.perform(get("/v1/users/me")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.phone").value("+233201234567"))
                    .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/avatar.png"))
                    .andExpect(jsonPath("$.data.language").value("en"))
                    .andExpect(jsonPath("$.data.timezone").value("Africa/Accra"))
                    .andExpect(jsonPath("$.data.currency").value("GHS"))
                    .andExpect(jsonPath("$.data.mfaEnabled").value(true));
        }
    }

    // =========================================================================
    // PATCH /v1/users/me
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/users/me")
    class UpdateMyProfile {

        @Test
        @DisplayName("200 — valid request body returns updated profile in $.data")
        void whenValidRequest_returns200WithUpdatedProfile() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Bob");
            request.setLastName("Jones");
            request.setTimezone("America/New_York");

            UserProfileResponse updated = UserProfileResponse.builder()
                    .id(principalId)
                    .email("alice@example.com")
                    .username("alice")
                    .firstName("Bob")
                    .lastName("Jones")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(userService.updateMyProfile(eq(principalId), any(UpdateProfileRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch("/v1/users/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile updated"))
                    .andExpect(jsonPath("$.data.firstName").value("Bob"))
                    .andExpect(jsonPath("$.data.lastName").value("Jones"));

            verify(userService).updateMyProfile(eq(principalId), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("200 — partial update (only firstName) is accepted")
        void whenOnlyFirstNameProvided_returns200() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Charlie");

            UserProfileResponse updated = UserProfileResponse.builder()
                    .id(principalId)
                    .email("alice@example.com")
                    .username("alice")
                    .firstName("Charlie")
                    .lastName("Smith")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(userService.updateMyProfile(eq(principalId), any(UpdateProfileRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch("/v1/users/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("Charlie"));
        }

        @Test
        @DisplayName("400 — firstName that violates @Size(min=1) is rejected")
        void whenFirstNameIsBlank_returns400() throws Exception {
            // An empty string violates @Size(min = 1, max = 100) on firstName
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("");

            mockMvc.perform(patch("/v1/users/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — invalid phone number format is rejected")
        void whenPhoneIsInvalid_returns400() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setPhone("not-a-phone");

            mockMvc.perform(patch("/v1/users/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — invalid currency code (lowercase) is rejected")
        void whenCurrencyIsLowercase_returns400() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setCurrency("usd"); // Must be ^[A-Z]{3}$

            mockMvc.perform(patch("/v1/users/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — invalid language code is rejected")
        void whenLanguageCodeIsInvalid_returns400() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setLanguage("EN-US"); // Must be ^[a-z]{2,8}$

            mockMvc.perform(patch("/v1/users/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 — unauthenticated PATCH is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Bob");

            mockMvc.perform(patch("/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // POST /v1/users/me/deactivate
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/users/me/deactivate")
    class DeactivateAccount {

        @Test
        @DisplayName("200 — confirmed deactivation request returns success with null data")
        void whenConfirmedRequest_returns200() throws Exception {
            AccountDeactivationRequest request = new AccountDeactivationRequest();
            request.setReason("Taking a break");
            request.setConfirm(true);

            doNothing().when(userService).deactivateAccount(eq(principalId), any(AccountDeactivationRequest.class));

            mockMvc.perform(post("/v1/users/me/deactivate")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account deactivated"));

            verify(userService).deactivateAccount(eq(principalId), any(AccountDeactivationRequest.class));
        }

        @Test
        @DisplayName("200 — confirm=false still passes controller validation (service enforces confirm)")
        void whenConfirmFalse_controllerAcceptsRequest() throws Exception {
            // @NotBlank is only on reason; confirm has no @NotNull/@AssertTrue — the
            // controller passes the request through and the service throws. The controller
            // itself returns 200 only if the service does not throw, so we test that
            // validation does NOT reject a false confirm at the controller layer.
            AccountDeactivationRequest request = new AccountDeactivationRequest();
            request.setReason("Some reason");
            request.setConfirm(false);

            doNothing().when(userService).deactivateAccount(any(), any());

            mockMvc.perform(post("/v1/users/me/deactivate")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 — missing reason field fails @NotBlank validation")
        void whenReasonIsMissing_returns400() throws Exception {
            // reason is omitted — @NotBlank will fire
            String body = "{\"confirm\":true}";

            mockMvc.perform(post("/v1/users/me/deactivate")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 — unauthenticated deactivation request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            AccountDeactivationRequest request = new AccountDeactivationRequest();
            request.setReason("Taking a break");
            request.setConfirm(true);

            mockMvc.perform(post("/v1/users/me/deactivate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // POST /v1/users/me/delete-request
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/users/me/delete-request")
    class RequestAccountDeletion {

        @Test
        @DisplayName("200 — confirmed deletion request returns scheduled-deletion message")
        void whenConfirmedRequest_returns200WithMessage() throws Exception {
            AccountDeletionRequest request = new AccountDeletionRequest();
            request.setReason("Leaving the platform");
            request.setConfirm(true);

            doNothing().when(userService).requestAccountDeletion(eq(principalId), any(AccountDeletionRequest.class));

            mockMvc.perform(post("/v1/users/me/delete-request")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(
                            "Deletion request recorded. Your account will be removed in 30 days."));

            verify(userService).requestAccountDeletion(eq(principalId), any(AccountDeletionRequest.class));
        }

        @Test
        @DisplayName("400 — missing reason field fails @NotBlank validation")
        void whenReasonIsMissing_returns400() throws Exception {
            String body = "{\"confirm\":true}";

            mockMvc.perform(post("/v1/users/me/delete-request")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 — unauthenticated delete-request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            AccountDeletionRequest request = new AccountDeletionRequest();
            request.setReason("Leaving the platform");
            request.setConfirm(true);

            mockMvc.perform(post("/v1/users/me/delete-request")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 — data field is null (no payload returned for deletion request)")
        void dataFieldIsNull() throws Exception {
            AccountDeletionRequest request = new AccountDeletionRequest();
            request.setReason("Leaving the platform");
            request.setConfirm(true);

            doNothing().when(userService).requestAccountDeletion(any(), any());

            mockMvc.perform(post("/v1/users/me/delete-request")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    // ApiResponse uses @JsonInclude(NON_NULL) so $.data is absent when null
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @TestConfiguration
    @Import(UserSecurityRules.class)
    static class TestSecurityConfig {

        @Autowired private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        @Autowired private CustomAccessDeniedHandler customAccessDeniedHandler;
        @Autowired private UserSecurityRules userSecurityRules;

        @Bean
        @Order(Integer.MIN_VALUE)
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(customAuthenticationEntryPoint)
                            .accessDeniedHandler(customAccessDeniedHandler))
                    .authorizeHttpRequests(auth -> {
                        userSecurityRules.configure(auth);
                        auth.anyRequest().authenticated();
                    })
                    .build();
        }
    }
}

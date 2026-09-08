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
import ecommerce.modules.user.UserSecurityRules;
import ecommerce.modules.user.dto.AdminSuspendRequest;
import ecommerce.modules.user.dto.AdminUserSearchParams;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests for {@link AdminUserController} using {@code @WebMvcTest} + MockMvc.
 *
 * <p>All endpoints require {@code ROLE_ADMIN}. Tests verify:
 * <ul>
 *   <li>200 with expected JSON shape for ADMIN-authenticated requests.</li>
 *   <li>403 when the caller holds a non-admin role (e.g. CUSTOMER).</li>
 *   <li>401 when no principal is present.</li>
 *   <li>Correct service method is called with the right arguments.</li>
 * </ul>
 */
@WebMvcTest(AdminUserController.class)
@ActiveProfiles("test")
@DisplayName("AdminUserController — /v1/admin/users")
class AdminUserControllerTest {

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

    private UUID adminId;
    private UUID targetUserId;
    private UserPrincipal adminPrincipal;
    private UserPrincipal customerPrincipal;
    private UserProfileResponse sampleProfile;

    @BeforeEach
    void setUp() throws Exception {
        adminId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        // ADMIN principal
        adminPrincipal = org.mockito.Mockito.mock(UserPrincipal.class);
        when(adminPrincipal.getId()).thenReturn(adminId);
        when(adminPrincipal.getUsername()).thenReturn("admin@example.com");
        when(adminPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> adminAuthorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(adminAuthorities).when(adminPrincipal).getAuthorities();
        when(adminPrincipal.isEnabled()).thenReturn(true);
        when(adminPrincipal.isAccountNonExpired()).thenReturn(true);
        when(adminPrincipal.isAccountNonLocked()).thenReturn(true);
        when(adminPrincipal.isCredentialsNonExpired()).thenReturn(true);

        // CUSTOMER principal (non-admin — used to verify 403 paths)
        customerPrincipal = org.mockito.Mockito.mock(UserPrincipal.class);
        when(customerPrincipal.getId()).thenReturn(UUID.randomUUID());
        when(customerPrincipal.getUsername()).thenReturn("customer@example.com");
        when(customerPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> customerAuthorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        doReturn(customerAuthorities).when(customerPrincipal).getAuthorities();
        when(customerPrincipal.isEnabled()).thenReturn(true);
        when(customerPrincipal.isAccountNonExpired()).thenReturn(true);
        when(customerPrincipal.isAccountNonLocked()).thenReturn(true);
        when(customerPrincipal.isCredentialsNonExpired()).thenReturn(true);

        sampleProfile = UserProfileResponse.builder()
                .id(targetUserId)
                .email("target@example.com")
                .username("targetuser")
                .firstName("Target")
                .lastName("User")
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
    // GET /v1/admin/users
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/admin/users — searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("200 — ADMIN receives paginated user list in $.data")
        void whenAdmin_returns200WithPage() throws Exception {
            Page<UserProfileResponse> page = new PageImpl<>(
                    List.of(sampleProfile),
                    PageRequest.of(0, 20),
                    1L);

            when(userService.adminSearchUsers(any(AdminUserSearchParams.class), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/admin/users")
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Users retrieved"))
                    .andExpect(jsonPath("$.data.content[0].id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.content[0].email").value("target@example.com"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(userService).adminSearchUsers(any(AdminUserSearchParams.class), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 — empty page is returned when no users match the query")
        void whenNoUsersMatch_returns200WithEmptyPage() throws Exception {
            Page<UserProfileResponse> empty = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 20),
                    0L);

            when(userService.adminSearchUsers(any(AdminUserSearchParams.class), any()))
                    .thenReturn(empty);

            mockMvc.perform(get("/v1/admin/users")
                            .param("query", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("403 — CUSTOMER role is denied access")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(get("/v1/admin/users")
                            .with(user(customerPrincipal)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/admin/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // GET /v1/admin/users/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/admin/users/{id} — getUser")
    class GetUser {

        @Test
        @DisplayName("200 — ADMIN retrieves a specific user profile by UUID")
        void whenAdmin_returns200WithProfile() throws Exception {
            when(userService.adminGetUser(targetUserId)).thenReturn(sampleProfile);

            mockMvc.perform(get("/v1/admin/users/{id}", targetUserId)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User retrieved"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.email").value("target@example.com"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

            verify(userService).adminGetUser(targetUserId);
        }

        @Test
        @DisplayName("403 — CUSTOMER role is denied access to another user's profile")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(get("/v1/admin/users/{id}", targetUserId)
                            .with(user(customerPrincipal)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/admin/users/{id}", targetUserId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 — all profile fields are present in the response")
        void responseContainsAllProfileFields() throws Exception {
            when(userService.adminGetUser(any(UUID.class))).thenReturn(sampleProfile);

            mockMvc.perform(get("/v1/admin/users/{id}", targetUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("targetuser"))
                    .andExpect(jsonPath("$.data.firstName").value("Target"))
                    .andExpect(jsonPath("$.data.lastName").value("User"))
                    .andExpect(jsonPath("$.data.emailVerified").value(true))
                    .andExpect(jsonPath("$.data.mfaEnabled").value(false));
        }
    }

    // =========================================================================
    // PATCH /v1/admin/users/{id}/suspend
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/suspend — suspendUser")
    class SuspendUser {

        @Test
        @DisplayName("200 — ADMIN can suspend a user; response reflects SUSPENDED status")
        void whenAdmin_returns200WithSuspendedProfile() throws Exception {
            AdminSuspendRequest request = new AdminSuspendRequest();
            request.setReason("Suspected fraud");
            request.setDurationDays(30);

            UserProfileResponse suspended = UserProfileResponse.builder()
                    .id(targetUserId)
                    .email("target@example.com")
                    .username("targetuser")
                    .firstName("Target")
                    .lastName("User")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.SUSPENDED)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(userService.adminSuspendUser(eq(targetUserId), eq(adminId), any(AdminSuspendRequest.class)))
                    .thenReturn(suspended);

            mockMvc.perform(patch("/v1/admin/users/{id}/suspend", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User suspended"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

            verify(userService).adminSuspendUser(eq(targetUserId), eq(adminId), any(AdminSuspendRequest.class));
        }

        @Test
        @DisplayName("400 — missing reason field fails @NotBlank validation")
        void whenReasonIsMissing_returns400() throws Exception {
            // reason is required (@NotBlank) on AdminSuspendRequest
            String body = "{\"durationDays\":30}";

            mockMvc.perform(patch("/v1/admin/users/{id}/suspend", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("403 — CUSTOMER role cannot suspend a user")
        void whenCustomer_returns403() throws Exception {
            AdminSuspendRequest request = new AdminSuspendRequest();
            request.setReason("Suspected fraud");

            mockMvc.perform(patch("/v1/admin/users/{id}/suspend", targetUserId)
                            .with(user(customerPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated suspend request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            AdminSuspendRequest request = new AdminSuspendRequest();
            request.setReason("Suspected fraud");

            mockMvc.perform(patch("/v1/admin/users/{id}/suspend", targetUserId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("200 — actorId is sourced from the authenticated principal (not path)")
        void actorIdComeFromPrincipal() throws Exception {
            AdminSuspendRequest request = new AdminSuspendRequest();
            request.setReason("Abuse");

            when(userService.adminSuspendUser(any(), any(), any())).thenReturn(sampleProfile);

            mockMvc.perform(patch("/v1/admin/users/{id}/suspend", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // The actorId passed to the service must equal the principal's UUID
            verify(userService).adminSuspendUser(eq(targetUserId), eq(adminId), any(AdminSuspendRequest.class));
        }
    }

    // =========================================================================
    // PATCH /v1/admin/users/{id}/activate
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/activate — activateUser")
    class ActivateUser {

        @Test
        @DisplayName("200 — ADMIN activates a user; response reflects ACTIVE status")
        void whenAdmin_returns200WithActiveProfile() throws Exception {
            UserProfileResponse activated = UserProfileResponse.builder()
                    .id(targetUserId)
                    .email("target@example.com")
                    .username("targetuser")
                    .firstName("Target")
                    .lastName("User")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(userService.adminActivateUser(targetUserId)).thenReturn(activated);

            mockMvc.perform(patch("/v1/admin/users/{id}/activate", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User activated"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            verify(userService).adminActivateUser(targetUserId);
        }

        @Test
        @DisplayName("200 — no request body is required (activate takes only the path variable)")
        void activateRequiresNoBody() throws Exception {
            when(userService.adminActivateUser(targetUserId)).thenReturn(sampleProfile);

            // Deliberately send no Content-Type / body
            mockMvc.perform(patch("/v1/admin/users/{id}/activate", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 — CUSTOMER role cannot activate a user")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/{id}/activate", targetUserId)
                            .with(user(customerPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated activate request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/{id}/activate", targetUserId)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // PATCH /v1/admin/users/{id}/disable
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/disable — disableUser")
    class DisableUser {

        @Test
        @DisplayName("200 — ADMIN disables a user with a reason; response reflects DISABLED status")
        void whenAdminWithReason_returns200WithDisabledProfile() throws Exception {
            UserProfileResponse disabled = UserProfileResponse.builder()
                    .id(targetUserId)
                    .email("target@example.com")
                    .username("targetuser")
                    .firstName("Target")
                    .lastName("User")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.DISABLED)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(userService.adminDisableUser(eq(targetUserId), eq("Permanent ban")))
                    .thenReturn(disabled);

            mockMvc.perform(patch("/v1/admin/users/{id}/disable", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .param("reason", "Permanent ban"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User disabled"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.status").value("DISABLED"));

            verify(userService).adminDisableUser(targetUserId, "Permanent ban");
        }

        @Test
        @DisplayName("200 — ADMIN disables a user without a reason (reason is optional)")
        void whenAdminWithoutReason_returns200() throws Exception {
            UserProfileResponse disabled = UserProfileResponse.builder()
                    .id(targetUserId)
                    .email("target@example.com")
                    .username("targetuser")
                    .firstName("Target")
                    .lastName("User")
                    .role(Role.CUSTOMER)
                    .status(UserStatus.DISABLED)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(userService.adminDisableUser(eq(targetUserId), isNull()))
                    .thenReturn(disabled);

            // No ?reason param — maps to null in the controller
            mockMvc.perform(patch("/v1/admin/users/{id}/disable", targetUserId)
                            .with(user(adminPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DISABLED"));

            verify(userService).adminDisableUser(eq(targetUserId), isNull());
        }

        @Test
        @DisplayName("403 — CUSTOMER role cannot disable a user")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/{id}/disable", targetUserId)
                            .with(user(customerPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated disable request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/{id}/disable", targetUserId)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 — reason query param is forwarded verbatim to the service")
        void reasonParamIsForwardedToService() throws Exception {
            when(userService.adminDisableUser(any(UUID.class), eq("Policy breach")))
                    .thenReturn(sampleProfile);

            mockMvc.perform(patch("/v1/admin/users/{id}/disable", targetUserId)
                            .with(csrf())
                            .param("reason", "Policy breach"))
                    .andExpect(status().isOk());

            verify(userService).adminDisableUser(any(UUID.class), eq("Policy breach"));
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

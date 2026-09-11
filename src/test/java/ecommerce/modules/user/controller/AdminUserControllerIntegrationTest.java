package ecommerce.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.user.dto.AdminSuspendRequest;
import ecommerce.modules.user.dto.AdminUserSearchParams;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link AdminUserController}.
 *
 * <p>Loads the complete Spring application context against an H2 in-memory
 * database. The service layer is replaced by a {@code @MockBean}. Authentication
 * is injected via {@code SecurityMockMvcRequestPostProcessors.user()} so the real
 * {@link ecommerce.common.config.SecurityConfig} filter chain makes role-based
 * decisions just as it would in production.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>{@code ROLE_ADMIN} receives 200 on all admin endpoints.</li>
 *   <li>{@code ROLE_CUSTOMER} receives 403 — enforced by {@code hasRole("ADMIN")} on each
 *       {@code @PreAuthorize} and by the URL-level rule in {@link ecommerce.modules.user.UserSecurityRules}.</li>
 *   <li>Unauthenticated requests receive 401.</li>
 * </ul>
 *
 * <p>Profile {@code test} activates H2, disables Liquibase, excludes Redis beans, and
 * uses Caffeine-only caching (see {@code src/test/resources/application-test.yaml}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminUserController — Integration Tests — /v1/admin/users")
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UUID adminId;
    private UUID targetUserId;
    private UserPrincipal adminPrincipal;
    private UserPrincipal customerPrincipal;
    private UserProfileResponse sampleProfile;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        // ── ADMIN principal ────────────────────────────────────────────────────
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

        // ── CUSTOMER principal (non-admin — used to verify 403 paths) ─────────
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
    }

    // =========================================================================
    // GET /v1/admin/users
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/admin/users — searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("200 — ADMIN receives paginated user list through the real security filter chain")
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
        }

        @Test
        @DisplayName("403 — CUSTOMER role is denied access by the real security filter chain")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(get("/v1/admin/users")
                            .with(user(customerPrincipal)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected by the real security filter chain")
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
        @DisplayName("200 — ADMIN can retrieve a specific user profile through the real security filter chain")
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
        }

        @Test
        @DisplayName("403 — CUSTOMER role is denied access to admin user lookup")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(get("/v1/admin/users/{id}", targetUserId)
                            .with(user(customerPrincipal)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected by the real security filter chain")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/admin/users/{id}", targetUserId))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // PATCH /v1/admin/users/{id}/suspend
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/suspend — suspendUser")
    class SuspendUser {

        @Test
        @DisplayName("200 — ADMIN can suspend a user through the real security filter chain")
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User suspended"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("403 — CUSTOMER role cannot suspend users")
        void whenCustomer_returns403() throws Exception {
            AdminSuspendRequest request = new AdminSuspendRequest();
            request.setReason("Suspected fraud");

            mockMvc.perform(patch("/v1/admin/users/{id}/suspend", targetUserId)
                            .with(user(customerPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PATCH /v1/admin/users/{id}/activate
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/activate — activateUser")
    class ActivateUser {

        @Test
        @DisplayName("200 — ADMIN can activate a user through the real security filter chain")
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
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User activated"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("403 — CUSTOMER role cannot activate users")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/{id}/activate", targetUserId)
                            .with(user(customerPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PATCH /v1/admin/users/{id}/disable
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/users/{id}/disable — disableUser")
    class DisableUser {

        @Test
        @DisplayName("200 — ADMIN can disable a user with a reason through the real security filter chain")
        void whenAdmin_returns200WithDisabledProfile() throws Exception {
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
                            .param("reason", "Permanent ban"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User disabled"))
                    .andExpect(jsonPath("$.data.id").value(targetUserId.toString()))
                    .andExpect(jsonPath("$.data.status").value("DISABLED"));
        }

        @Test
        @DisplayName("403 — CUSTOMER role cannot disable users")
        void whenCustomer_returns403() throws Exception {
            mockMvc.perform(patch("/v1/admin/users/{id}/disable", targetUserId)
                            .with(user(customerPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }
}

package ecommerce.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.user.dto.AccountDeactivationRequest;
import ecommerce.modules.user.dto.AccountDeletionRequest;
import ecommerce.modules.user.dto.UpdateProfileRequest;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link MeController}.
 *
 * <p>Loads the complete Spring application context (including the real
 * {@link ecommerce.common.config.SecurityConfig} filter chain) against an H2
 * in-memory database. The service layer is replaced by a {@code @MockBean} so
 * no real DB writes occur. Authentication is injected via
 * {@code SecurityMockMvcRequestPostProcessors.user()} which populates the
 * {@code SecurityContext} directly — the JWT filter sees a non-null context and
 * skips token extraction.
 *
 * <p>Profile {@code test} activates:
 * <ul>
 *   <li>H2 datasource with {@code create-drop} DDL</li>
 *   <li>Liquibase disabled</li>
 *   <li>Redis beans excluded ({@code @Profile("!test")} on {@code RedisConfig})</li>
 *   <li>Caffeine cache only</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MeController — Integration Tests — /v1/users/me")
class MeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UUID principalId;
    private UserPrincipal mockPrincipal;
    private UserProfileResponse sampleProfile;

    @BeforeEach
    void setUp() {
        principalId = UUID.randomUUID();

        // Build a mocked UserPrincipal. doReturn(...).when(...) is used for
        // getAuthorities() to avoid wildcard generic capture issues.
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
    }

    // =========================================================================
    // GET /v1/users/me
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/users/me")
    class GetMyProfile {

        @Test
        @DisplayName("200 — authenticated user can retrieve their profile via the full security chain")
        void whenAuthenticated_returns200WithProfile() throws Exception {
            when(userService.getMyProfile(principalId)).thenReturn(sampleProfile);

            mockMvc.perform(get("/v1/users/me")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile retrieved"))
                    .andExpect(jsonPath("$.data.id").value(principalId.toString()))
                    .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.emailVerified").value(true))
                    .andExpect(jsonPath("$.data.mfaEnabled").value(false));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected by the real security filter chain")
        void whenUnauthenticated_returns401() throws Exception {
            // No .with(user(...)) — JWT filter finds no token and SecurityContext is empty.
            mockMvc.perform(get("/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // PATCH /v1/users/me
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/users/me")
    class UpdateMyProfile {

        @Test
        @DisplayName("200 — authenticated user can update their profile through the real filter chain")
        void whenAuthenticated_returns200WithUpdatedProfile() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Bob");
            request.setLastName("Jones");

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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile updated"))
                    .andExpect(jsonPath("$.data.firstName").value("Bob"))
                    .andExpect(jsonPath("$.data.lastName").value("Jones"));
        }

        @Test
        @DisplayName("401 — unauthenticated PATCH is rejected by the real security filter chain")
        void whenUnauthenticated_returns401() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Bob");

            mockMvc.perform(patch("/v1/users/me")
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
        @DisplayName("200 — authenticated user can deactivate their account through the real filter chain")
        void whenAuthenticated_returns200() throws Exception {
            AccountDeactivationRequest request = new AccountDeactivationRequest();
            request.setReason("Taking a break");
            request.setConfirm(true);

            doNothing().when(userService).deactivateAccount(eq(principalId), any(AccountDeactivationRequest.class));

            mockMvc.perform(post("/v1/users/me/deactivate")
                            .with(user(mockPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Account deactivated"));
        }

        @Test
        @DisplayName("401 — unauthenticated deactivation request is rejected by the real security filter chain")
        void whenUnauthenticated_returns401() throws Exception {
            AccountDeactivationRequest request = new AccountDeactivationRequest();
            request.setReason("Taking a break");
            request.setConfirm(true);

            mockMvc.perform(post("/v1/users/me/deactivate")
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
        @DisplayName("200 — authenticated user can submit a deletion request through the real filter chain")
        void whenAuthenticated_returns200WithDeletionMessage() throws Exception {
            AccountDeletionRequest request = new AccountDeletionRequest();
            request.setReason("Leaving the platform");
            request.setConfirm(true);

            doNothing().when(userService).requestAccountDeletion(eq(principalId), any(AccountDeletionRequest.class));

            mockMvc.perform(post("/v1/users/me/delete-request")
                            .with(user(mockPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(
                            "Deletion request recorded. Your account will be removed in 30 days."));
        }

        @Test
        @DisplayName("401 — unauthenticated delete-request is rejected by the real security filter chain")
        void whenUnauthenticated_returns401() throws Exception {
            AccountDeletionRequest request = new AccountDeletionRequest();
            request.setReason("Leaving the platform");
            request.setConfirm(true);

            mockMvc.perform(post("/v1/users/me/delete-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}

package ecommerce.modules.authz.service;

import ecommerce.common.enums.ScopeType;
import ecommerce.modules.authz.entity.RoleEntity;
import ecommerce.modules.authz.entity.UserRoleEntity;
import ecommerce.modules.authz.repository.UserRoleEntityRepository;
import ecommerce.modules.authz.service.impl.AuthorizationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationServiceImpl Tests")
class AuthorizationServiceImplTest {

    @Mock
    private UserRoleEntityRepository userRoleEntityRepository;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    private UUID userId;
    private UUID scopeId;
    private static final String PERMISSION_CODE = "PRODUCT_CREATE";

    @BeforeEach
    void setUp() {
        userId  = UUID.randomUUID();
        scopeId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── hasPermission(userId, code) ───────────────────────────────────────────

    @Nested
    @DisplayName("hasPermission(userId, permissionCode)")
    class HasPermissionSimpleTests {

        @Test
        @DisplayName("Should return false when SecurityContext has no authentication")
        void hasPermission_WhenAuthNull_ReturnsFalse() {
            SecurityContextHolder.clearContext();

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when authentication is not authenticated")
        void hasPermission_WhenNotAuthenticated_ReturnsFalse() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when authority is present")
        void hasPermission_WhenAuthorityPresent_ReturnsTrue() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when authority is absent")
        void hasPermission_WhenAuthorityAbsent_ReturnsFalse() {
            setAuthenticationWithAuthority("OTHER_PERMISSION");

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE);

            assertThat(result).isFalse();
        }
    }

    // ── hasPermission(userId, code, scopeType, scopeId) ───────────────────────

    @Nested
    @DisplayName("hasPermission(userId, permissionCode, scopeType, scopeId)")
    class HasPermissionScopedTests {

        @Test
        @DisplayName("Should return false when base permission is missing")
        void hasPermission_WhenBasePermissionMissing_ReturnsFalse() {
            setAuthenticationWithAuthority("OTHER");

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE, ScopeType.STORE, scopeId);

            assertThat(result).isFalse();
            verify(userRoleEntityRepository, never())
                    .findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(any(), any(), any());
        }

        @Test
        @DisplayName("Should return true when base permission present and scoped role has no expiry")
        void hasPermission_WhenBasePresentAndScopedRoleNoExpiry_ReturnsTrue() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            UserRoleEntity ur = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(buildRole())
                    .scopeType(ScopeType.STORE)
                    .scopeId(scopeId)
                    .expiresAt(null)
                    .active(true)
                    .build();

            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of(ur));

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE, ScopeType.STORE, scopeId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when scoped role is expired")
        void hasPermission_WhenScopedRoleExpired_ReturnsFalse() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            UserRoleEntity ur = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(buildRole())
                    .scopeType(ScopeType.STORE)
                    .scopeId(scopeId)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .active(true)
                    .build();

            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of(ur));

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE, ScopeType.STORE, scopeId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when no scoped role found")
        void hasPermission_WhenNoScopedRole_ReturnsFalse() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of());

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE, ScopeType.STORE, scopeId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when scoped role has future expiry")
        void hasPermission_WhenScopedRoleNotYetExpired_ReturnsTrue() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            UserRoleEntity ur = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(buildRole())
                    .scopeType(ScopeType.STORE)
                    .scopeId(scopeId)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .active(true)
                    .build();

            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of(ur));

            boolean result = authorizationService.hasPermission(userId, PERMISSION_CODE, ScopeType.STORE, scopeId);

            assertThat(result).isTrue();
        }
    }

    // ── hasScope ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasScope")
    class HasScopeTests {

        @Test
        @DisplayName("Should return true when repository returns non-empty list")
        void hasScope_WhenAssignmentsFound_ReturnsTrue() {
            UserRoleEntity ur = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(buildRole())
                    .scopeType(ScopeType.STORE)
                    .scopeId(scopeId)
                    .active(true)
                    .build();

            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of(ur));

            boolean result = authorizationService.hasScope(userId, ScopeType.STORE, scopeId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when repository returns empty list")
        void hasScope_WhenNoAssignments_ReturnsFalse() {
            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of());

            boolean result = authorizationService.hasScope(userId, ScopeType.STORE, scopeId);

            assertThat(result).isFalse();
        }
    }

    // ── authorize(userId, code) ───────────────────────────────────────────────

    @Nested
    @DisplayName("authorize(userId, permissionCode)")
    class AuthorizeSimpleTests {

        @Test
        @DisplayName("Should throw AccessDeniedException when permission is absent")
        void authorize_WhenPermissionMissing_ThrowsAccessDeniedException() {
            setAuthenticationWithAuthority("OTHER");

            assertThatThrownBy(() -> authorizationService.authorize(userId, PERMISSION_CODE))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(PERMISSION_CODE);
        }

        @Test
        @DisplayName("Should not throw when permission is present")
        void authorize_WhenPermissionPresent_DoesNotThrow() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            // Should complete without throwing
            authorizationService.authorize(userId, PERMISSION_CODE);
        }
    }

    // ── authorize(userId, code, scope, scopeId) ───────────────────────────────

    @Nested
    @DisplayName("authorize(userId, permissionCode, scopeType, scopeId)")
    class AuthorizeScopedTests {

        @Test
        @DisplayName("Should throw AccessDeniedException when scoped check fails")
        void authorize_WhenScopedPermissionMissing_ThrowsAccessDeniedException() {
            setAuthenticationWithAuthority("OTHER");

            assertThatThrownBy(() -> authorizationService.authorize(userId, PERMISSION_CODE, ScopeType.STORE, scopeId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining(PERMISSION_CODE);
        }

        @Test
        @DisplayName("Should not throw when scoped permission is present")
        void authorize_WhenScopedPermissionPresent_DoesNotThrow() {
            setAuthenticationWithAuthority(PERMISSION_CODE);

            UserRoleEntity ur = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(buildRole())
                    .scopeType(ScopeType.STORE)
                    .scopeId(scopeId)
                    .expiresAt(null)
                    .active(true)
                    .build();

            when(userRoleEntityRepository.findByUserIdAndScopeTypeAndScopeIdAndActiveTrue(userId, ScopeType.STORE, scopeId))
                    .thenReturn(List.of(ur));

            // Should complete without throwing
            authorizationService.authorize(userId, PERMISSION_CODE, ScopeType.STORE, scopeId);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setAuthenticationWithAuthority(String authority) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private RoleEntity buildRole() {
        return RoleEntity.builder()
                .id(UUID.randomUUID())
                .code("SELLER")
                .displayName("Seller")
                .active(true)
                .build();
    }
}

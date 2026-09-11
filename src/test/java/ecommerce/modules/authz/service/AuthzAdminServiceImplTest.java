package ecommerce.modules.authz.service;

import ecommerce.common.enums.ScopeType;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.DuplicateResourceException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.common.util.TokenValidationService;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.authz.dto.*;
import ecommerce.modules.authz.entity.Permission;
import ecommerce.modules.authz.entity.RoleEntity;
import ecommerce.modules.authz.entity.RolePermission;
import ecommerce.modules.authz.entity.UserRoleEntity;
import ecommerce.modules.authz.mapper.AuthzMapper;
import ecommerce.modules.authz.repository.PermissionRepository;
import ecommerce.modules.authz.repository.RoleEntityRepository;
import ecommerce.modules.authz.repository.RolePermissionRepository;
import ecommerce.modules.authz.repository.UserRoleEntityRepository;
import ecommerce.modules.authz.service.impl.AuthzAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthzAdminServiceImpl Tests")
class AuthzAdminServiceImplTest {

    @Mock
    private RoleEntityRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleEntityRepository userRoleRepository;

    @Mock
    private AuthzMapper mapper;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TokenValidationService tokenValidationService;

    @InjectMocks
    private AuthzAdminServiceImpl authzAdminService;

    private UUID roleId;
    private UUID permissionId;
    private UUID userId;
    private UUID actorId;
    private RoleEntity role;
    private RoleEntity systemRole;
    private Permission permission;
    private RoleDto roleDto;
    private PermissionDto permissionDto;
    private UserRoleDto userRoleDto;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        permissionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        role = RoleEntity.builder()
                .id(roleId)
                .code("SELLER")
                .displayName("Seller")
                .description("Seller role")
                .systemDefined(false)
                .active(true)
                .build();

        systemRole = RoleEntity.builder()
                .id(UUID.randomUUID())
                .code("ADMIN")
                .displayName("Admin")
                .description("System admin")
                .systemDefined(true)
                .active(true)
                .build();

        permission = Permission.builder()
                .id(permissionId)
                .code("PRODUCT_CREATE")
                .resource("PRODUCT")
                .action("CREATE")
                .build();

        roleDto = RoleDto.builder()
                .id(roleId)
                .code("SELLER")
                .displayName("Seller")
                .systemDefined(false)
                .active(true)
                .permissions(List.of())
                .build();

        permissionDto = PermissionDto.builder()
                .id(permissionId)
                .code("PRODUCT_CREATE")
                .resource("PRODUCT")
                .action("CREATE")
                .build();

        userRoleDto = UserRoleDto.builder()
                .id(UUID.randomUUID())
                .roleCode("SELLER")
                .scopeType(ScopeType.GLOBAL)
                .active(true)
                .build();
    }

    // ── listRoles ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listRoles")
    class ListRolesTests {

        @Test
        @DisplayName("Should return page of role DTOs from repository")
        void listRoles_ReturnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<RoleEntity> entityPage = new PageImpl<>(List.of(role));
            when(roleRepository.findAll(pageable)).thenReturn(entityPage);
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            Page<RoleDto> result = authzAdminService.listRoles(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCode()).isEqualTo("SELLER");
            verify(roleRepository).findAll(pageable);
            verify(mapper).toRoleDto(role);
        }

        @Test
        @DisplayName("Should return empty page when no roles exist")
        void listRoles_WhenNoRoles_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(roleRepository.findAll(pageable)).thenReturn(Page.empty());

            Page<RoleDto> result = authzAdminService.listRoles(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ── getRole ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRole")
    class GetRoleTests {

        @Test
        @DisplayName("Should return RoleDto when role found")
        void getRole_WhenFound_ReturnsDto() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            RoleDto result = authzAdminService.getRole(roleId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(roleId);
            assertThat(result.getCode()).isEqualTo("SELLER");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when role not found")
        void getRole_WhenNotFound_ThrowsException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authzAdminService.getRole(roleId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(roleId.toString());
        }
    }

    // ── createRole ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRole")
    class CreateRoleTests {

        @Test
        @DisplayName("Should create and return RoleDto when code is unique")
        void createRole_WhenCodeUnique_SavesAndReturnsDto() {
            CreateRoleRequest request = new CreateRoleRequest();
            request.setCode("SELLER");
            request.setDisplayName("Seller");
            request.setDescription("Seller role");

            when(roleRepository.existsByCode("SELLER")).thenReturn(false);
            when(roleRepository.save(any(RoleEntity.class))).thenReturn(role);
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            RoleDto result = authzAdminService.createRole(request);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("SELLER");
            verify(roleRepository).save(any(RoleEntity.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when code already exists")
        void createRole_WhenCodeDuplicate_ThrowsException() {
            CreateRoleRequest request = new CreateRoleRequest();
            request.setCode("SELLER");

            when(roleRepository.existsByCode("SELLER")).thenReturn(true);

            assertThatThrownBy(() -> authzAdminService.createRole(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("SELLER");

            verify(roleRepository, never()).save(any());
        }
    }

    // ── updateRole ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRole")
    class UpdateRoleTests {

        @Test
        @DisplayName("Should patch fields and return updated RoleDto")
        void updateRole_WhenFound_PatchesAndSaves() {
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setDisplayName("Updated Seller");
            request.setDescription("Updated description");
            request.setActive(false);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(roleRepository.save(role)).thenReturn(role);
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            RoleDto result = authzAdminService.updateRole(roleId, request);

            assertThat(result).isNotNull();
            assertThat(role.getDisplayName()).isEqualTo("Updated Seller");
            assertThat(role.getDescription()).isEqualTo("Updated description");
            assertThat(role.isActive()).isFalse();
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("Should not overwrite fields when update request fields are null")
        void updateRole_WhenNullFields_DoesNotOverwrite() {
            UpdateRoleRequest request = new UpdateRoleRequest();
            // all fields null

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(roleRepository.save(role)).thenReturn(role);
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            authzAdminService.updateRole(roleId, request);

            assertThat(role.getDisplayName()).isEqualTo("Seller");
            assertThat(role.getDescription()).isEqualTo("Seller role");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when role not found")
        void updateRole_WhenNotFound_ThrowsException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authzAdminService.updateRole(roleId, new UpdateRoleRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── deleteRole ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRole")
    class DeleteRoleTests {

        @Test
        @DisplayName("Should delete non-system role successfully")
        void deleteRole_WhenNonSystemRole_Deletes() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

            authzAdminService.deleteRole(roleId);

            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("Should throw BadRequestException when deleting system-defined role")
        void deleteRole_WhenSystemRole_ThrowsException() {
            UUID systemRoleId = systemRole.getId();
            when(roleRepository.findById(systemRoleId)).thenReturn(Optional.of(systemRole));

            assertThatThrownBy(() -> authzAdminService.deleteRole(systemRoleId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("system-defined");

            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when role not found")
        void deleteRole_WhenNotFound_ThrowsException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authzAdminService.deleteRole(roleId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── grantPermission ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("grantPermission")
    class GrantPermissionTests {

        @Test
        @DisplayName("Should save RolePermission and log audit when not already granted")
        void grantPermission_WhenNotAlreadyGranted_SavesAndAudits() {
            GrantPermissionRequest request = new GrantPermissionRequest();
            request.setRoleCode("SELLER");
            request.setPermissionCode("PRODUCT_CREATE");

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(permissionRepository.findByCode("PRODUCT_CREATE")).thenReturn(Optional.of(permission));
            when(rolePermissionRepository.existsByRole_IdAndPermission_Id(roleId, permissionId)).thenReturn(false);
            when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(RolePermission.builder().role(role).permission(permission).build());
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            RoleDto result = authzAdminService.grantPermission(request);

            assertThat(result).isNotNull();
            verify(rolePermissionRepository).save(any(RolePermission.class));

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.PERMISSION_GRANTED);
        }

        @Test
        @DisplayName("Should not save duplicate RolePermission when already granted")
        void grantPermission_WhenAlreadyGranted_NoDuplicateSave() {
            GrantPermissionRequest request = new GrantPermissionRequest();
            request.setRoleCode("SELLER");
            request.setPermissionCode("PRODUCT_CREATE");

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(permissionRepository.findByCode("PRODUCT_CREATE")).thenReturn(Optional.of(permission));
            when(rolePermissionRepository.existsByRole_IdAndPermission_Id(roleId, permissionId)).thenReturn(true);
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(mapper.toRoleDto(role)).thenReturn(roleDto);

            authzAdminService.grantPermission(request);

            verify(rolePermissionRepository, never()).save(any());
            verify(auditLogService, never()).log(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when permission code not found")
        void grantPermission_WhenPermissionNotFound_ThrowsException() {
            GrantPermissionRequest request = new GrantPermissionRequest();
            request.setRoleCode("SELLER");
            request.setPermissionCode("UNKNOWN");

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(permissionRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authzAdminService.grantPermission(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");
        }
    }

    // ── revokePermission ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("revokePermission")
    class RevokePermissionTests {

        @Test
        @DisplayName("Should delete RolePermission and log audit when found")
        void revokePermission_WhenFound_DeletesAndAudits() {
            RevokePermissionRequest request = new RevokePermissionRequest();
            request.setRoleCode("SELLER");
            request.setPermissionCode("PRODUCT_CREATE");

            RolePermission rp = RolePermission.builder().role(role).permission(permission).build();

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(permissionRepository.findByCode("PRODUCT_CREATE")).thenReturn(Optional.of(permission));
            when(rolePermissionRepository.findByRole_IdAndPermission_Id(roleId, permissionId)).thenReturn(Optional.of(rp));

            authzAdminService.revokePermission(request);

            verify(rolePermissionRepository).delete(rp);
            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.PERMISSION_REVOKED);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when permission code not found")
        void revokePermission_WhenPermissionNotFound_ThrowsException() {
            RevokePermissionRequest request = new RevokePermissionRequest();
            request.setRoleCode("SELLER");
            request.setPermissionCode("UNKNOWN");

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(permissionRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authzAdminService.revokePermission(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");

            verify(rolePermissionRepository, never()).delete(any());
        }
    }

    // ── assignRole ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("assignRole")
    class AssignRoleTests {

        @Test
        @DisplayName("Should save UserRoleEntity, evict principal, and log ROLE_ASSIGNED when no expiresAt")
        void assignRole_WithoutExpiry_LogsRoleAssigned() {
            AssignRoleRequest request = new AssignRoleRequest();
            request.setUserId(userId);
            request.setRoleCode("SELLER");
            request.setScopeType(ScopeType.GLOBAL);
            request.setExpiresAt(null);

            UserRoleEntity saved = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(role)
                    .scopeType(ScopeType.GLOBAL)
                    .active(true)
                    .build();

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(userRoleRepository.save(any(UserRoleEntity.class))).thenReturn(saved);
            when(mapper.toUserRoleDto(saved)).thenReturn(userRoleDto);

            UserRoleDto result = authzAdminService.assignRole(request, actorId);

            assertThat(result).isNotNull();
            verify(userRoleRepository).save(any(UserRoleEntity.class));
            verify(tokenValidationService).evictPrincipal(userId);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.ROLE_ASSIGNED);
        }

        @Test
        @DisplayName("Should log TEMPORARY_ACCESS_GRANTED when expiresAt is set")
        void assignRole_WithExpiry_LogsTemporaryAccessGranted() {
            Instant future = Instant.now().plusSeconds(3600);
            AssignRoleRequest request = new AssignRoleRequest();
            request.setUserId(userId);
            request.setRoleCode("SELLER");
            request.setScopeType(ScopeType.STORE);
            request.setScopeId(UUID.randomUUID());
            request.setExpiresAt(future);

            UserRoleEntity saved = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(role)
                    .scopeType(ScopeType.STORE)
                    .expiresAt(future)
                    .active(true)
                    .build();

            when(roleRepository.findByCode("SELLER")).thenReturn(Optional.of(role));
            when(userRoleRepository.save(any(UserRoleEntity.class))).thenReturn(saved);
            when(mapper.toUserRoleDto(saved)).thenReturn(userRoleDto);

            authzAdminService.assignRole(request, actorId);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.TEMPORARY_ACCESS_GRANTED);
        }
    }

    // ── revokeRole ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("revokeRole")
    class RevokeRoleTests {

        @Test
        @DisplayName("Should set active=false, evict principal, and log ROLE_REVOKED when assignment found")
        void revokeRole_WhenFound_RevokesAndEvicts() {
            RevokeRoleRequest request = new RevokeRoleRequest();
            request.setUserId(userId);
            request.setRoleCode("SELLER");
            request.setScopeType(ScopeType.GLOBAL);

            UserRoleEntity assignment = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(role)
                    .scopeType(ScopeType.GLOBAL)
                    .active(true)
                    .build();

            when(userRoleRepository.findByUserIdAndRole_CodeAndScopeTypeAndActiveTrue(userId, "SELLER", ScopeType.GLOBAL))
                    .thenReturn(Optional.of(assignment));
            when(userRoleRepository.save(assignment)).thenReturn(assignment);

            authzAdminService.revokeRole(request, actorId);

            assertThat(assignment.isActive()).isFalse();
            verify(userRoleRepository).save(assignment);
            verify(tokenValidationService).evictPrincipal(userId);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.ROLE_REVOKED);
        }

        @Test
        @DisplayName("Should be a no-op when no active assignment found")
        void revokeRole_WhenNoActiveAssignment_NoOp() {
            RevokeRoleRequest request = new RevokeRoleRequest();
            request.setUserId(userId);
            request.setRoleCode("SELLER");
            request.setScopeType(ScopeType.GLOBAL);

            when(userRoleRepository.findByUserIdAndRole_CodeAndScopeTypeAndActiveTrue(userId, "SELLER", ScopeType.GLOBAL))
                    .thenReturn(Optional.empty());

            authzAdminService.revokeRole(request, actorId);

            verify(userRoleRepository, never()).save(any());
            verify(tokenValidationService, never()).evictPrincipal(any());
            verify(auditLogService, never()).log(any());
        }
    }

    // ── getUserRoles ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserRoles")
    class GetUserRolesTests {

        @Test
        @DisplayName("Should delegate to repository and map results")
        void getUserRoles_DelegatesToRepoAndMaps() {
            UserRoleEntity assignment = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .role(role)
                    .active(true)
                    .build();

            when(userRoleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(assignment));
            when(mapper.toUserRoleDto(assignment)).thenReturn(userRoleDto);

            List<UserRoleDto> result = authzAdminService.getUserRoles(userId);

            assertThat(result).hasSize(1);
            verify(userRoleRepository).findByUserIdAndActiveTrue(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no active roles")
        void getUserRoles_WhenNoRoles_ReturnsEmptyList() {
            when(userRoleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

            List<UserRoleDto> result = authzAdminService.getUserRoles(userId);

            assertThat(result).isEmpty();
        }
    }

    // ── listPermissions ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("listPermissions")
    class ListPermissionsTests {

        @Test
        @DisplayName("Should delegate to repository and map all permissions")
        void listPermissions_DelegatesToRepoAndMaps() {
            when(permissionRepository.findAll()).thenReturn(List.of(permission));
            when(mapper.toPermissionDto(permission)).thenReturn(permissionDto);

            List<PermissionDto> result = authzAdminService.listPermissions();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("PRODUCT_CREATE");
            verify(permissionRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no permissions exist")
        void listPermissions_WhenNone_ReturnsEmptyList() {
            when(permissionRepository.findAll()).thenReturn(List.of());

            List<PermissionDto> result = authzAdminService.listPermissions();

            assertThat(result).isEmpty();
        }
    }
}

package ecommerce.modules.authz.service.impl;

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
import ecommerce.modules.authz.service.AuthzAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthzAdminServiceImpl implements AuthzAdminService {

    private final RoleEntityRepository     roleRepository;
    private final PermissionRepository     permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleEntityRepository userRoleRepository;
    private final AuthzMapper              mapper;
    private final AuditLogService          auditLogService;
    private final TokenValidationService   tokenValidationService;

    // ── Roles ─────────────────────────────────────────────────────────────────

    @Override
    public Page<RoleDto> listRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(mapper::toRoleDto);
    }

    @Override
    public RoleDto getRole(UUID id) {
        return mapper.toRoleDto(findRole(id));
    }

    @Override
    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Role code already exists: " + request.getCode());
        }
        RoleEntity role = RoleEntity.builder()
                .code(request.getCode())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .build();
        return mapper.toRoleDto(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleDto updateRole(UUID id, UpdateRoleRequest request) {
        RoleEntity role = findRole(id);
        if (request.getDisplayName() != null) role.setDisplayName(request.getDisplayName());
        if (request.getDescription()  != null) role.setDescription(request.getDescription());
        if (request.getActive()       != null) role.setActive(request.getActive());
        return mapper.toRoleDto(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        RoleEntity role = findRole(id);
        if (role.isSystemDefined()) {
            throw new BadRequestException("Cannot delete system-defined role: " + role.getCode());
        }
        roleRepository.delete(role);
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoleDto grantPermission(GrantPermissionRequest request) {
        RoleEntity role = findRoleByCode(request.getRoleCode());
        Permission perm = permissionRepository.findByCode(request.getPermissionCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found: " + request.getPermissionCode()));

        if (!rolePermissionRepository.existsByRole_IdAndPermission_Id(role.getId(), perm.getId())) {
            rolePermissionRepository.save(
                    RolePermission.builder().role(role).permission(perm).build());
            auditLogService.log(AuditLogEntry.builder()
                    .action(AuditAction.PERMISSION_GRANTED)
                    .entityType("ROLE")
                    .entityPublicId(role.getId())
                    .reason("Granted " + perm.getCode() + " to " + role.getCode())
                    .status(AuditLogEntry.STATUS_SUCCESS)
                    .build());
        }
        // reload to pick up the new RolePermission
        return mapper.toRoleDto(roleRepository.findById(role.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public void revokePermission(RevokePermissionRequest request) {
        RoleEntity role = findRoleByCode(request.getRoleCode());
        Permission perm = permissionRepository.findByCode(request.getPermissionCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permission not found: " + request.getPermissionCode()));

        rolePermissionRepository.findByRole_IdAndPermission_Id(role.getId(), perm.getId())
                .ifPresent(rp -> {
                    rolePermissionRepository.delete(rp);
                    auditLogService.log(AuditLogEntry.builder()
                            .action(AuditAction.PERMISSION_REVOKED)
                            .entityType("ROLE")
                            .entityPublicId(role.getId())
                            .reason("Revoked " + perm.getCode() + " from " + role.getCode())
                            .status(AuditLogEntry.STATUS_SUCCESS)
                            .build());
                });
    }

    // ── User-Role assignments ─────────────────────────────────────────────────

    @Override
    @Transactional
    public UserRoleDto assignRole(AssignRoleRequest request, UUID actorId) {
        RoleEntity role = findRoleByCode(request.getRoleCode());
        UserRoleEntity userRole = UserRoleEntity.builder()
                .userId(request.getUserId())
                .role(role)
                .scopeType(request.getScopeType())
                .scopeId(request.getScopeId())
                .assignedBy(actorId)
                .expiresAt(request.getExpiresAt())
                .build();
        UserRoleEntity saved = userRoleRepository.save(userRole);
        tokenValidationService.evictPrincipal(request.getUserId());

        String action = request.getExpiresAt() != null
                ? AuditAction.TEMPORARY_ACCESS_GRANTED
                : AuditAction.ROLE_ASSIGNED;
        auditLogService.log(AuditLogEntry.builder()
                .action(action)
                .entityType("USER")
                .entityPublicId(request.getUserId())
                .reason("Assigned " + role.getCode() + " scope=" + request.getScopeType())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toUserRoleDto(saved);
    }

    @Override
    @Transactional
    public void revokeRole(RevokeRoleRequest request, UUID actorId) {
        userRoleRepository
                .findByUserIdAndRole_CodeAndScopeTypeAndActiveTrue(
                        request.getUserId(), request.getRoleCode(), request.getScopeType())
                .ifPresent(ur -> {
                    ur.setActive(false);
                    userRoleRepository.save(ur);
                    tokenValidationService.evictPrincipal(request.getUserId());
                    auditLogService.log(AuditLogEntry.builder()
                            .action(AuditAction.ROLE_REVOKED)
                            .entityType("USER")
                            .entityPublicId(request.getUserId())
                            .reason("Revoked " + request.getRoleCode())
                            .status(AuditLogEntry.STATUS_SUCCESS)
                            .build());
                });
    }

    @Override
    public List<UserRoleDto> getUserRoles(UUID userId) {
        return userRoleRepository.findByUserIdAndActiveTrue(userId)
                .stream().map(mapper::toUserRoleDto).toList();
    }

    @Override
    public List<PermissionDto> listPermissions() {
        return permissionRepository.findAll()
                .stream().map(mapper::toPermissionDto).toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RoleEntity findRole(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private RoleEntity findRoleByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + code));
    }
}

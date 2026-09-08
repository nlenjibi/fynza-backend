package ecommerce.modules.authz.mapper;

import ecommerce.modules.authz.dto.PermissionDto;
import ecommerce.modules.authz.dto.RoleDto;
import ecommerce.modules.authz.dto.UserRoleDto;
import ecommerce.modules.authz.entity.Permission;
import ecommerce.modules.authz.entity.RoleEntity;
import ecommerce.modules.authz.entity.RolePermission;
import ecommerce.modules.authz.entity.UserRoleEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class AuthzMapper {

    public PermissionDto toPermissionDto(Permission permission) {
        if (permission == null) return null;
        return PermissionDto.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .build();
    }

    public RoleDto toRoleDto(RoleEntity role) {
        if (role == null) return null;
        return RoleDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .systemDefined(role.isSystemDefined())
                .active(role.isActive())
                .permissions(toPermissionCodes(role.getRolePermissions()))
                .build();
    }

    public List<String> toPermissionCodes(Set<RolePermission> rps) {
        if (rps == null) return List.of();
        return rps.stream().map(rp -> rp.getPermission().getCode()).toList();
    }

    public UserRoleDto toUserRoleDto(UserRoleEntity userRole) {
        if (userRole == null) return null;
        return UserRoleDto.builder()
                .id(userRole.getId())
                .roleCode(userRole.getRole().getCode())
                .roleDisplayName(userRole.getRole().getDisplayName())
                .scopeType(userRole.getScopeType())
                .scopeId(userRole.getScopeId())
                .assignedBy(userRole.getAssignedBy())
                .assignedAt(userRole.getAssignedAt())
                .expiresAt(userRole.getExpiresAt())
                .active(userRole.isActive())
                .build();
    }
}

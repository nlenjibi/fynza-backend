package ecommerce.modules.authz.service;

import ecommerce.modules.authz.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AuthzAdminService {

    Page<RoleDto>       listRoles(Pageable pageable);
    RoleDto             getRole(UUID id);
    RoleDto             createRole(CreateRoleRequest request);
    RoleDto             updateRole(UUID id, UpdateRoleRequest request);
    void                deleteRole(UUID id);

    RoleDto             grantPermission(GrantPermissionRequest request);
    void                revokePermission(RevokePermissionRequest request);

    UserRoleDto         assignRole(AssignRoleRequest request, UUID actorId);
    void                revokeRole(RevokeRoleRequest request, UUID actorId);
    List<UserRoleDto>   getUserRoles(UUID userId);

    List<PermissionDto> listPermissions();
}

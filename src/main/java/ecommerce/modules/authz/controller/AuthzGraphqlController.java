package ecommerce.modules.authz.controller;

import ecommerce.common.enums.ScopeType;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.authz.dto.*;
import ecommerce.modules.authz.service.AuthzAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthzGraphqlController {

    private final AuthzAdminService authzAdminService;

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public List<RoleDto> roles() {
        log.debug("GQL roles");
        return authzAdminService.listRoles(PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public RoleDto role(@Argument String id) {
        log.debug("GQL role id={}", id);
        return authzAdminService.getRole(UUID.fromString(id));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public List<PermissionDto> permissions() {
        log.debug("GQL permissions");
        return authzAdminService.listPermissions();
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public List<UserRoleDto> userRoles(@Argument String userId) {
        log.debug("GQL userRoles userId={}", userId);
        return authzAdminService.getUserRoles(UUID.fromString(userId));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public RoleDto createRole(@Argument Map<String, Object> input) {
        log.debug("GQL createRole input={}", input);
        CreateRoleRequest req = new CreateRoleRequest();
        req.setCode((String) input.get("code"));
        req.setDisplayName((String) input.get("displayName"));
        req.setDescription((String) input.get("description"));
        return authzAdminService.createRole(req);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public RoleDto updateRole(@Argument String id, @Argument Map<String, Object> input) {
        log.debug("GQL updateRole id={}", id);
        UpdateRoleRequest req = new UpdateRoleRequest();
        req.setDisplayName((String) input.get("displayName"));
        req.setDescription((String) input.get("description"));
        if (input.containsKey("active")) {
            req.setActive((Boolean) input.get("active"));
        }
        return authzAdminService.updateRole(UUID.fromString(id), req);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public boolean deleteRole(@Argument String id) {
        log.debug("GQL deleteRole id={}", id);
        authzAdminService.deleteRole(UUID.fromString(id));
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public UserRoleDto assignRole(@Argument Map<String, Object> input,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL assignRole input={}", input);
        AssignRoleRequest req = new AssignRoleRequest();
        req.setUserId(UUID.fromString((String) input.get("userId")));
        req.setRoleCode((String) input.get("roleCode"));
        req.setScopeType(ScopeType.valueOf((String) input.get("scopeType")));
        if (input.get("scopeId") != null) {
            req.setScopeId(UUID.fromString((String) input.get("scopeId")));
        }
        if (input.get("expiresAt") != null) {
            req.setExpiresAt(Instant.parse((String) input.get("expiresAt")));
        }
        return authzAdminService.assignRole(req, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public boolean revokeRole(@Argument Map<String, Object> input,
                              @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL revokeRole input={}", input);
        RevokeRoleRequest req = new RevokeRoleRequest();
        req.setUserId(UUID.fromString((String) input.get("userId")));
        req.setRoleCode((String) input.get("roleCode"));
        req.setScopeType(ScopeType.valueOf((String) input.get("scopeType")));
        if (input.get("scopeId") != null) {
            req.setScopeId(UUID.fromString((String) input.get("scopeId")));
        }
        authzAdminService.revokeRole(req, principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public RoleDto grantPermission(@Argument Map<String, Object> input) {
        log.debug("GQL grantPermission input={}", input);
        GrantPermissionRequest req = new GrantPermissionRequest();
        req.setRoleCode((String) input.get("roleCode"));
        req.setPermissionCode((String) input.get("permissionCode"));
        return authzAdminService.grantPermission(req);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('role.manage')")
    public boolean revokePermission(@Argument Map<String, Object> input) {
        log.debug("GQL revokePermission input={}", input);
        RevokePermissionRequest req = new RevokePermissionRequest();
        req.setRoleCode((String) input.get("roleCode"));
        req.setPermissionCode((String) input.get("permissionCode"));
        authzAdminService.revokePermission(req);
        return true;
    }
}

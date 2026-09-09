package ecommerce.modules.authz.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.authz.dto.*;
import ecommerce.modules.authz.service.AuthzAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/authz")
@PreAuthorize("hasAuthority('role.manage')")
@RequiredArgsConstructor
public class AdminAuthzController {

    private final AuthzAdminService authzAdminService;

    // ── Roles ─────────────────────────────────────────────────────────────────

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<Page<RoleDto>>> listRoles(Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success("Roles retrieved", authzAdminService.listRoles(pageable)));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        RoleDto created = authzAdminService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created", created));
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> getRole(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Role retrieved", authzAdminService.getRole(id)));
    }

    @PatchMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable UUID id,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Role updated", authzAdminService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        authzAdminService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted", null));
    }

    // ── Role ↔ Permission ────────────────────────────────────────────────────

    @PostMapping("/roles/{id}/permissions")
    public ResponseEntity<ApiResponse<RoleDto>> grantPermission(
            @PathVariable UUID id,
            @Valid @RequestBody GrantPermissionRequest request) {
        // resolve the roleCode from the path id so the service uses the canonical code
        RoleDto role = authzAdminService.getRole(id);
        request.setRoleCode(role.getCode());
        return ResponseEntity.ok(
                ApiResponse.success("Permission granted", authzAdminService.grantPermission(request)));
    }

    @DeleteMapping("/roles/{id}/permissions")
    public ResponseEntity<ApiResponse<Void>> revokePermission(
            @PathVariable UUID id,
            @Valid @RequestBody RevokePermissionRequest request) {
        RoleDto role = authzAdminService.getRole(id);
        request.setRoleCode(role.getCode());
        authzAdminService.revokePermission(request);
        return ResponseEntity.ok(ApiResponse.success("Permission revoked", null));
    }

    // ── Permissions catalogue ─────────────────────────────────────────────────

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> listPermissions() {
        return ResponseEntity.ok(
                ApiResponse.success("Permissions retrieved", authzAdminService.listPermissions()));
    }

    // ── User-role assignments ─────────────────────────────────────────────────

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<UserRoleDto>> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        request.setUserId(userId);
        UserRoleDto dto = authzAdminService.assignRole(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role assigned", dto));
    }

    @DeleteMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<Void>> revokeRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RevokeRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        request.setUserId(userId);
        authzAdminService.revokeRole(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Role revoked", null));
    }

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<List<UserRoleDto>>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                ApiResponse.success("User roles retrieved", authzAdminService.getUserRoles(userId)));
    }
}

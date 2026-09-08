package ecommerce.modules.user.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.user.dto.AdminSuspendRequest;
import ecommerce.modules.user.dto.AdminUserSearchParams;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — User Management", description = "Admin operations for searching and managing user accounts")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users", description = "Admin: search and list users with optional filters")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> searchUsers(
            @ParameterObject AdminUserSearchParams params,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.adminSearchUsers(params, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Admin: fetch full profile for a specific user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userService.adminGetUser(id)));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend user", description = "Admin: suspend a user account, revoking all active sessions")
    public ResponseEntity<ApiResponse<UserProfileResponse>> suspendUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminSuspendRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User suspended", userService.adminSuspendUser(id, principal.getId(), request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate user", description = "Admin: reactivate a suspended or disabled user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User activated", userService.adminActivateUser(id)));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable user", description = "Admin: permanently disable a user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> disableUser(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success("User disabled", userService.adminDisableUser(id, reason)));
    }
}

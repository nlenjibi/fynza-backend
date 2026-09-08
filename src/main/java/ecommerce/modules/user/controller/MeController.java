package ecommerce.modules.user.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.user.dto.AccountDeactivationRequest;
import ecommerce.modules.user.dto.AccountDeletionRequest;
import ecommerce.modules.user.dto.UpdateProfileRequest;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "My Account", description = "Authenticated user self-service endpoints")
public class MeController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Returns the full profile for the authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", userService.getMyProfile(principal.getId())));
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my profile", description = "Updates editable profile fields for the authenticated user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateMyProfile(principal.getId(), request)));
    }

    @PostMapping("/deactivate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate my account", description = "Deactivates the authenticated user's account and revokes all sessions")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccountDeactivationRequest request) {
        userService.deactivateAccount(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }

    @PostMapping("/delete-request")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request account deletion", description = "Schedules permanent account deletion after the 30-day grace period")
    public ResponseEntity<ApiResponse<Void>> requestAccountDeletion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccountDeletionRequest request) {
        userService.requestAccountDeletion(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Deletion request recorded. Your account will be removed in 30 days.", null));
    }
}

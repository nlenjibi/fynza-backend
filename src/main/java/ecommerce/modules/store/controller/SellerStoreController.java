package ecommerce.modules.store.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.store.dto.request.*;
import ecommerce.modules.store.dto.response.*;
import ecommerce.modules.store.service.StoreManagementService;
import ecommerce.modules.store.service.StorePolicyService;
import ecommerce.modules.store.service.StoreSettingService;
import ecommerce.modules.store.service.StoreStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sellers/me/stores")
@RequiredArgsConstructor
@Tag(name = "Store — Seller Self-service", description = "Seller store management")
public class SellerStoreController {

    private final StoreManagementService storeManagementService;
    private final StoreStatusService     storeStatusService;
    private final StoreSettingService    storeSettingService;
    private final StorePolicyService     storePolicyService;

    // ── Store lifecycle ───────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('store.create.own')")
    @Operation(summary = "Create a new store")
    public ResponseEntity<ApiResponse<StoreDetailResponse>> createStore(
            @Valid @RequestBody CreateStoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store created successfully",
                        storeManagementService.createStore(principal.getId(), request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('store.read.own')")
    @Operation(summary = "Get my store details")
    public ResponseEntity<ApiResponse<StoreDetailResponse>> getMyStore(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Store retrieved",
                storeManagementService.getMyStore(principal.getId())));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('store.update.own')")
    @Operation(summary = "Update my store profile")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Valid @RequestBody UpdateStoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Store updated",
                storeManagementService.updateStore(principal.getId(), request)));
    }

    @PostMapping("/me/submit")
    @PreAuthorize("hasAuthority('store.publish.own')")
    @Operation(summary = "Submit store for admin review")
    public ResponseEntity<ApiResponse<StoreResponse>> submitForReview(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Store submitted for review",
                storeManagementService.submitForReview(principal.getId())));
    }

    @PatchMapping("/me/visibility")
    @PreAuthorize("hasAuthority('store.publish.own')")
    @Operation(summary = "Update store visibility")
    public ResponseEntity<ApiResponse<StoreResponse>> updateVisibility(
            @Valid @RequestBody StoreVisibilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Visibility updated",
                storeManagementService.updateVisibility(principal.getId(), request)));
    }

    @PostMapping("/me/pause")
    @PreAuthorize("hasAuthority('store.pause.own')")
    @Operation(summary = "Pause my store")
    public ResponseEntity<ApiResponse<StoreResponse>> pauseStore(
            @RequestBody(required = false) StoreStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID storePublicId = storeManagementService.getMyStore(principal.getId()).getId();
        StoreStatusRequest pauseRequest = request != null ? request : new StoreStatusRequest();
        pauseRequest.setStatus(ecommerce.modules.store.enums.StoreStatus.PAUSED);
        return ResponseEntity.ok(ApiResponse.success("Store paused",
                storeStatusService.changeStatus(storePublicId, principal.getId(), pauseRequest)));
    }

    @PostMapping("/me/close")
    @PreAuthorize("hasAuthority('store.delete.own')")
    @Operation(summary = "Close my store")
    public ResponseEntity<ApiResponse<StoreResponse>> closeStore(
            @RequestBody(required = false) StoreStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID storePublicId = storeManagementService.getMyStore(principal.getId()).getId();
        StoreStatusRequest closeRequest = request != null ? request : new StoreStatusRequest();
        closeRequest.setStatus(ecommerce.modules.store.enums.StoreStatus.CLOSED);
        return ResponseEntity.ok(ApiResponse.success("Store closed",
                storeStatusService.changeStatus(storePublicId, principal.getId(), closeRequest)));
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    @GetMapping("/me/settings")
    @PreAuthorize("hasAuthority('store.manage.own')")
    @Operation(summary = "Get store settings")
    public ResponseEntity<ApiResponse<StoreSettingResponse>> getSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved",
                storeSettingService.getSettings(principal.getId())));
    }

    @PatchMapping("/me/settings")
    @PreAuthorize("hasAuthority('store.manage.own')")
    @Operation(summary = "Update store settings")
    public ResponseEntity<ApiResponse<StoreSettingResponse>> updateSettings(
            @Valid @RequestBody StoreSettingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Settings updated",
                storeSettingService.updateSettings(principal.getId(), request)));
    }

    // ── Policies ──────────────────────────────────────────────────────────────

    @GetMapping("/me/policies")
    @PreAuthorize("hasAuthority('store.manage.own')")
    @Operation(summary = "List store policies")
    public ResponseEntity<ApiResponse<List<StorePolicyResponse>>> getPolicies(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Policies retrieved",
                storePolicyService.getPolicies(principal.getId())));
    }

    @PostMapping("/me/policies")
    @PreAuthorize("hasAuthority('store.manage.own')")
    @Operation(summary = "Create a store policy")
    public ResponseEntity<ApiResponse<StorePolicyResponse>> createPolicy(
            @Valid @RequestBody StorePolicyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Policy created",
                        storePolicyService.createPolicy(principal.getId(), request)));
    }

    @PatchMapping("/me/policies/{policyId}")
    @PreAuthorize("hasAuthority('store.manage.own')")
    @Operation(summary = "Update a store policy")
    public ResponseEntity<ApiResponse<StorePolicyResponse>> updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody StorePolicyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Policy updated",
                storePolicyService.updatePolicy(principal.getId(), policyId, request)));
    }

    @DeleteMapping("/me/policies/{policyId}")
    @PreAuthorize("hasAuthority('store.manage.own')")
    @Operation(summary = "Delete a store policy")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @PathVariable UUID policyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        storePolicyService.deletePolicy(principal.getId(), policyId);
        return ResponseEntity.ok(ApiResponse.success("Policy deleted", null));
    }
}

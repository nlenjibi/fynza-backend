package ecommerce.modules.seller.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.seller.dto.request.SellerStatusRequest;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.dto.response.SellerVerificationResponse;
import ecommerce.modules.seller.enums.VerificationType;
import ecommerce.modules.seller.service.SellerStatusService;
import ecommerce.modules.seller.service.SellerVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/sellers")
@RequiredArgsConstructor
@Tag(name = "Admin — Seller Management", description = "Admin operations for managing sellers")
public class AdminSellerController {

    private final SellerStatusService       statusService;
    private final SellerVerificationService verificationService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve seller application")
    public ResponseEntity<ApiResponse<SellerResponse>> approveSeller(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Seller approved",
                statusService.approveSeller(id, principal.getId())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject seller application")
    public ResponseEntity<ApiResponse<SellerResponse>> rejectSeller(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) SellerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Seller rejected",
                statusService.rejectSeller(id, principal.getId(), request != null ? request : new SellerStatusRequest())));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend seller")
    public ResponseEntity<ApiResponse<SellerResponse>> suspendSeller(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Seller suspended",
                statusService.suspendSeller(id, principal.getId(), request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate seller")
    public ResponseEntity<ApiResponse<SellerResponse>> activateSeller(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Seller activated",
                statusService.activateSeller(id, principal.getId())));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block seller")
    public ResponseEntity<ApiResponse<SellerResponse>> blockSeller(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Seller blocked",
                statusService.blockSeller(id, principal.getId(), request)));
    }

    @PostMapping("/{id}/verification/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Review seller verification")
    public ResponseEntity<ApiResponse<SellerVerificationResponse>> reviewVerification(
            @PathVariable UUID id,
            @RequestParam VerificationType type,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Verification reviewed",
                verificationService.adminReviewVerification(id, type, approved, reason, principal.getId())));
    }
}

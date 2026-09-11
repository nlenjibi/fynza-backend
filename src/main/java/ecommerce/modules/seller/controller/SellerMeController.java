package ecommerce.modules.seller.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.seller.dto.request.SellerOnboardingRequest;
import ecommerce.modules.seller.dto.request.UpdateSellerRequest;
import ecommerce.modules.seller.dto.response.SellerDetailResponse;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.dto.response.SellerVerificationResponse;
import ecommerce.modules.seller.enums.VerificationType;
import ecommerce.modules.seller.service.SellerOnboardingService;
import ecommerce.modules.seller.service.SellerService;
import ecommerce.modules.seller.service.SellerVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller — Self-service", description = "Seller profile, onboarding and verification")
public class SellerMeController {

    private final SellerService            sellerService;
    private final SellerOnboardingService  onboardingService;
    private final SellerVerificationService verificationService;

    // ── Profile ───────────────────────────────────────────────────────────────

    @PatchMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update my seller profile")
    public ResponseEntity<ApiResponse<SellerResponse>> updateMySeller(
            @Valid @RequestBody UpdateSellerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Seller profile updated",
                sellerService.updateMySeller(principal.getId(), request)));
    }

    // ── Onboarding ────────────────────────────────────────────────────────────

    @PostMapping("/me/onboarding")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Save onboarding business information")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> saveOnboarding(
            @RequestBody SellerOnboardingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Onboarding information saved",
                onboardingService.saveOnboarding(principal.getId(), request)));
    }

    @PatchMapping("/me/onboarding")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update onboarding business information")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> updateOnboarding(
            @RequestBody SellerOnboardingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Onboarding information updated",
                onboardingService.saveOnboarding(principal.getId(), request)));
    }

    @PostMapping("/me/application")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Submit seller application for review")
    public ResponseEntity<ApiResponse<SellerResponse>> submitApplication(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Application submitted for review",
                onboardingService.submitApplication(principal.getId())));
    }

    // ── Verification ─────────────────────────────────────────────────────────

    @PostMapping("/me/verification")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Submit a verification request")
    public ResponseEntity<ApiResponse<SellerVerificationResponse>> submitVerification(
            @RequestParam VerificationType type,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Verification submitted",
                verificationService.submitVerification(principal.getId(), type)));
    }
}

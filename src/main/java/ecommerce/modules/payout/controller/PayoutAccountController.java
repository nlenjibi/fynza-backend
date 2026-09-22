package ecommerce.modules.payout.controller;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.payout.dto.request.AddPayoutAccountRequest;
import ecommerce.modules.payout.dto.request.UpdatePayoutAccountRequest;
import ecommerce.modules.payout.dto.response.PayoutAccountResponse;
import ecommerce.modules.payout.service.PayoutAccountService;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/payout-accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seller — Payout Accounts", description = "Manage seller payout bank/mobile accounts")
public class PayoutAccountController {

    private final PayoutAccountService payoutAccountService;
    private final SellerRepository sellerRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('payout:account:write')")
    @Operation(summary = "Add a payout account")
    public ResponseEntity<ApiResponse<PayoutAccountResponse>> addAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddPayoutAccountRequest request) {

        Long sellerId = resolveSellerId(principal);
        PayoutAccountResponse response = payoutAccountService.addAccount(sellerId, request);
        log.info("Payout account added sellerId={}", sellerId);
        return ResponseEntity.ok(ApiResponse.success("Payout account added", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('payout:account:write')")
    @Operation(summary = "Update a payout account")
    public ResponseEntity<ApiResponse<PayoutAccountResponse>> updateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody UpdatePayoutAccountRequest request) {

        Long sellerId = resolveSellerId(principal);
        PayoutAccountResponse response = payoutAccountService.updateAccount(sellerId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Payout account updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('payout:account:write')")
    @Operation(summary = "Remove a payout account")
    public ResponseEntity<ApiResponse<Void>> removeAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        Long sellerId = resolveSellerId(principal);
        payoutAccountService.removeAccount(sellerId, id);
        return ResponseEntity.ok(ApiResponse.success("Payout account removed", null));
    }

    @PostMapping("/{id}/set-default")
    @PreAuthorize("hasAuthority('payout:account:write')")
    @Operation(summary = "Set a payout account as default")
    public ResponseEntity<ApiResponse<PayoutAccountResponse>> setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        Long sellerId = resolveSellerId(principal);
        PayoutAccountResponse response = payoutAccountService.setDefault(sellerId, id);
        return ResponseEntity.ok(ApiResponse.success("Default payout account updated", response));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveSellerId(UserPrincipal principal) {
        Seller seller = sellerRepository.findByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return seller.getId();
    }
}

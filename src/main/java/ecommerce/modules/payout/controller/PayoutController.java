package ecommerce.modules.payout.controller;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.payout.dto.request.RequestPayoutRequest;
import ecommerce.modules.payout.dto.response.PayoutResponse;
import ecommerce.modules.payout.service.PayoutService;
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
@RequestMapping("/v1/seller/payouts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seller — Payouts", description = "Seller payout requests")
public class PayoutController {

    private final PayoutService payoutService;
    private final SellerRepository sellerRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('payout:write')")
    @Operation(summary = "Request a payout")
    public ResponseEntity<ApiResponse<PayoutResponse>> requestPayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RequestPayoutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long sellerId = resolveSellerId(principal);
        String effectiveKey = idempotencyKey != null ? idempotencyKey : request.getIdempotencyKey();

        PayoutResponse response = payoutService.requestPayout(
                sellerId, request.getPayoutAccountId(), request.getAmount(), effectiveKey);

        log.info("Payout requested sellerId={} amount={}", sellerId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Payout request submitted", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('payout:write')")
    @Operation(summary = "Cancel a payout request")
    public ResponseEntity<ApiResponse<PayoutResponse>> cancelPayout(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        Long sellerId = resolveSellerId(principal);
        PayoutResponse response = payoutService.cancelPayout(sellerId, id);
        log.info("Payout cancelled sellerId={} payoutId={}", sellerId, id);
        return ResponseEntity.ok(ApiResponse.success("Payout cancelled", response));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveSellerId(UserPrincipal principal) {
        Seller seller = sellerRepository.findByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return seller.getId();
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}

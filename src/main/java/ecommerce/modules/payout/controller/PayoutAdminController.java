package ecommerce.modules.payout.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.payout.dto.response.PayoutResponse;
import ecommerce.modules.payout.service.PayoutAccountService;
import ecommerce.modules.payout.service.PayoutAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/payouts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin — Payouts", description = "Admin payout lifecycle management")
public class PayoutAdminController {

    private final PayoutAdminService payoutAdminService;
    private final PayoutAccountService payoutAccountService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('payout:admin')")
    @Operation(summary = "Approve a payout request")
    public ResponseEntity<ApiResponse<PayoutResponse>> approvePayout(@PathVariable UUID id) {
        PayoutResponse response = payoutAdminService.approvePayout(id);
        log.info("Payout approved by admin payoutId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Payout approved", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('payout:admin')")
    @Operation(summary = "Reject a payout request")
    public ResponseEntity<ApiResponse<PayoutResponse>> rejectPayout(
            @PathVariable UUID id,
            @RequestBody RejectPayoutRequest request) {
        PayoutResponse response = payoutAdminService.rejectPayout(id, request.getReason());
        log.info("Payout rejected by admin payoutId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Payout rejected", response));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('payout:admin')")
    @Operation(summary = "Place a payout on hold")
    public ResponseEntity<ApiResponse<PayoutResponse>> holdPayout(
            @PathVariable UUID id,
            @RequestBody HoldPayoutRequest request) {
        PayoutResponse response = payoutAdminService.holdPayout(id, request.getReason());
        log.info("Payout placed on hold by admin payoutId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Payout placed on hold", response));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('payout:admin')")
    @Operation(summary = "Retry a failed payout")
    public ResponseEntity<ApiResponse<PayoutResponse>> retryPayout(@PathVariable UUID id) {
        PayoutResponse response = payoutAdminService.retryPayout(id);
        log.info("Payout queued for retry by admin payoutId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Payout queued for retry", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('payout:admin')")
    @Operation(summary = "Cancel a payout")
    public ResponseEntity<ApiResponse<PayoutResponse>> cancelPayout(@PathVariable UUID id) {
        PayoutResponse response = payoutAdminService.cancelPayout(id);
        log.info("Payout cancelled by admin payoutId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Payout cancelled", response));
    }

    @PostMapping("/accounts/{id}/verify")
    @PreAuthorize("hasAuthority('payout:admin')")
    @Operation(summary = "Verify a seller payout account")
    public ResponseEntity<ApiResponse<ecommerce.modules.payout.dto.response.PayoutAccountResponse>> verifyAccount(
            @PathVariable UUID id) {
        var response = payoutAccountService.verifyAccount(id);
        log.info("Payout account verified by admin accountId={}", id);
        return ResponseEntity.ok(ApiResponse.success("Payout account verified", response));
    }

    // ── Inline request classes ─────────────────────────────────────────────────

    @Data
    public static class RejectPayoutRequest {
        private String reason;
    }

    @Data
    public static class HoldPayoutRequest {
        private String reason;
    }
}

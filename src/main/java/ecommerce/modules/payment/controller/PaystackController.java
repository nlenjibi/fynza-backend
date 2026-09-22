package ecommerce.modules.payment.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.util.IdempotencyService;
import ecommerce.modules.payment.provider.dto.PaymentInitiateRequest;
import ecommerce.modules.payment.provider.dto.PaymentInitiateResult;
import ecommerce.modules.payment.provider.dto.PaymentVerifyResult;
import ecommerce.modules.payment.provider.dto.RefundResult;
import ecommerce.modules.payment.service.PaystackPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Payment REST controller. Routes through the active {@link ecommerce.modules.payment.provider.PaymentProvider}.
 * Switch providers via {@code fynza.payment.provider} — no code change required.
 */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaystackController {

    private final PaystackPaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PaymentInitiateResult>> initializePayment(
            @Valid @RequestBody InitializePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.validatePayload(idempotencyKey, request)) {
                return ResponseEntity.badRequest().body(ApiResponse.<PaymentInitiateResult>builder()
                        .message("Idempotency key already used with different payload").build());
            }
        }

        PaymentInitiateResult result = paymentService.initializePayment(
                PaymentInitiateRequest.builder()
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .email(request.getEmail())
                        .reference(request.getReference())
                        .callbackUrl(request.getCallbackUrl())
                        .metadata(request.getMetadata())
                        .description(request.getDescription())
                        .build());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(idempotencyKey, request, result);
        }

        return ResponseEntity.ok(ApiResponse.<PaymentInitiateResult>builder()
                .data(result).message("Payment initialized successfully").build());
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentVerifyResult>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        PaymentVerifyResult result = paymentService.verifyPayment(request.getReference());
        return ResponseEntity.ok(ApiResponse.<PaymentVerifyResult>builder()
                .data(result).message("Payment verified").build());
    }

    @PostMapping("/refund/{reference}")
    public ResponseEntity<ApiResponse<RefundResult>> processRefund(
            @PathVariable String reference,
            @RequestBody(required = false) RefundPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.validatePayload(idempotencyKey, reference)) {
                return ResponseEntity.badRequest().body(ApiResponse.<RefundResult>builder()
                        .message("Idempotency key already used with different payload").build());
            }
        }

        BigDecimal amount = request != null ? request.getAmount() : null;
        RefundResult result = paymentService.processRefund(reference, amount);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(idempotencyKey, reference, result);
        }

        return ResponseEntity.ok(ApiResponse.<RefundResult>builder()
                .data(result).message("Refund processed successfully").build());
    }

    // ── Inline request DTOs ────────────────────────────────────────────────────

    @Data
    public static class InitializePaymentRequest {
        @NotNull @Positive
        private BigDecimal amount;
        @NotBlank
        private String email;
        @NotBlank
        private String currency = "GHS";
        private String reference;
        private String callbackUrl;
        private String metadata;
        private String description;
    }

    @Data
    public static class VerifyPaymentRequest {
        @NotBlank
        private String reference;
    }

    @Data
    public static class RefundPaymentRequest {
        private BigDecimal amount;
        private String reason;
    }
}

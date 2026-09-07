package ecommerce.modules.payment.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.util.IdempotencyService;
import ecommerce.modules.payment.dto.PaystackInitializeRequest;
import ecommerce.modules.payment.dto.PaystackInitializeResponse;
import ecommerce.modules.payment.dto.PaystackVerifyResponse;
import ecommerce.modules.payment.service.PaystackPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST controller for Paystack payment operations.
 */
@RestController
@RequestMapping("/v1/payments/paystack")
@RequiredArgsConstructor
@Slf4j
public class PaystackController {

    private final PaystackPaymentService paystackPaymentService;
    private final IdempotencyService idempotencyService;

    /**
     * Initialize a payment transaction.
     * Accepts an Idempotency-Key header to prevent duplicate payment initializations.
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PaystackInitializeResponse>> initializePayment(
            @Valid @RequestBody PaystackInitializeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Payment initialization request for amount: {} {}", request.getAmount(), request.getCurrency());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.validatePayload(idempotencyKey, request)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<PaystackInitializeResponse>builder()
                                .message("Idempotency key already used with different payload")
                                .build());
            }
            idempotencyService.check(idempotencyKey).ifPresent(cached -> log.debug("Returning cached payment init for key={}", idempotencyKey));
        }

        PaystackInitializeResponse response = paystackPaymentService.initializePayment(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(idempotencyKey, request, response);
        }

        return ResponseEntity.ok(ApiResponse.<PaystackInitializeResponse>builder()
                .data(response)
                .message("Payment initialized successfully")
                .build());
    }

    /**
     * Verify a payment transaction.
     */
    @GetMapping("/verify/{reference}")
    public ResponseEntity<ApiResponse<PaystackVerifyResponse>> verifyPayment(
            @PathVariable String reference) {

        log.info("Payment verification request for reference: {}", reference);

        PaystackVerifyResponse response = paystackPaymentService.verifyPayment(reference);

        return ResponseEntity.ok(ApiResponse.<PaystackVerifyResponse>builder()
                .data(response)
                .message("Payment verified successfully")
                .build());
    }

    /**
     * Process a refund for a transaction.
     * Accepts an Idempotency-Key header to prevent duplicate refunds.
     */
    @PostMapping("/refund/{reference}")
    public ResponseEntity<ApiResponse<String>> processRefund(
            @PathVariable String reference,
            @RequestBody(required = false) Map<String, BigDecimal> request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Refund request for reference: {}", reference);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.validatePayload(idempotencyKey, request)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>builder()
                                .message("Idempotency key already used with different payload")
                                .build());
            }
        }

        BigDecimal amount = request != null ? request.get("amount") : null;
        String result = paystackPaymentService.processRefund(reference, amount);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(idempotencyKey, request, result);
        }

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .data(result)
                .message("Refund processed successfully")
                .build());
    }
}
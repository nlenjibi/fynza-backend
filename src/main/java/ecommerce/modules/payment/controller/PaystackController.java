package ecommerce.modules.payment.controller;

import ecommerce.common.response.ApiResponse;
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

    /**
     * Initialize a payment transaction.
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PaystackInitializeResponse>> initializePayment(
            @Valid @RequestBody PaystackInitializeRequest request) {

        log.info("Payment initialization request for amount: {} {}", request.getAmount(), request.getCurrency());

        PaystackInitializeResponse response = paystackPaymentService.initializePayment(request);

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
     */
    @PostMapping("/refund/{reference}")
    public ResponseEntity<ApiResponse<String>> processRefund(
            @PathVariable String reference,
            @RequestBody(required = false) Map<String, BigDecimal> request) {

        log.info("Refund request for reference: {}", reference);

        BigDecimal amount = request != null ? request.get("amount") : null;
        String result = paystackPaymentService.processRefund(reference, amount);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .data(result)
                .message("Refund processed successfully")
                .build());
    }
}
package ecommerce.modules.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.payment.config.PaystackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling Paystack webhook events.
 */
@RestController
@RequestMapping("/v1/webhooks/paystack")
@RequiredArgsConstructor
@Slf4j
public class PaystackWebhookController {

    private final PaystackProperties paystackProperties;
    private final ObjectMapper objectMapper;

    /**
     * Handle Paystack webhook events.
     * Events handled: charge.success, charge.failed, refund.created
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        log.info("Received Paystack webhook");

        // Verify webhook signature in production
        // String computedSignature = HMAC_SHA256.sign(payload, paystackProperties.getWebhookSecret());
        // if (!signature.equals(computedSignature)) {
        //     log.warn("Invalid webhook signature");
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        // }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.has("event") ? event.get("event").asText() : "";

            log.info("Processing webhook event: {}", eventType);

            switch (eventType) {
                case "charge.success":
                    handleChargeSuccess(event);
                    break;
                case "charge.failed":
                    handleChargeFailed(event);
                    break;
                case "refund.created":
                    handleRefundCreated(event);
                    break;
                default:
                    log.info("Unhandled webhook event: {}", eventType);
            }

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    /**
     * Handle successful payment event.
     */
    private void handleChargeSuccess(JsonNode event) {
        log.info("Payment successful");
        JsonNode data = event.has("data") ? event.get("data") : null;
        if (data != null) {
            String reference = data.has("reference") ? data.get("reference").asText() : "";
            String status = data.has("status") ? data.get("status").asText() : "";
            String transactionId = data.has("id") ? data.get("id").asText() : "";

            log.info("Transaction successful - Reference: {}, Status: {}, TransactionId: {}",
                    reference, status, transactionId);

            // TODO: Update payment status in database
            // TODO: Trigger order fulfillment
        }
    }

    /**
     * Handle failed payment event.
     */
    private void handleChargeFailed(JsonNode event) {
        log.warn("Payment failed");
        JsonNode data = event.has("data") ? event.get("data") : null;
        if (data != null) {
            String reference = data.has("reference") ? data.get("reference").asText() : "";
            String reason = data.has("message") ? data.get("message").asText() : "Unknown";

            log.warn("Transaction failed - Reference: {}, Reason: {}", reference, reason);

            // TODO: Update payment status in database
            // TODO: Notify user of failure
        }
    }

    /**
     * Handle refund created event.
     */
    private void handleRefundCreated(JsonNode event) {
        log.info("Refund created");
        JsonNode data = event.has("data") ? event.get("data") : null;
        if (data != null) {
            String reference = data.has("transaction") ? data.get("transaction").asText() : "";
            String refundId = data.has("id") ? data.get("id").asText() : "";

            log.info("Refund created - Transaction: {}, RefundId: {}", reference, refundId);

            // TODO: Update payment status to REFUNDED
            // TODO: Trigger order cancellation if applicable
        }
    }
}
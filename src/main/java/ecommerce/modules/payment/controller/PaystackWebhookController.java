package ecommerce.modules.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.payment.provider.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Webhook controller for payment gateway callbacks.
 *
 * Endpoint: POST /v1/payments/webhooks/{provider}
 * Signature verification is delegated to the active {@link PaymentProvider}.
 * Only webhooks whose {provider} path matches the active provider are accepted.
 */
@RestController
@RequestMapping("/v1/payments/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaystackWebhookController {

    private final PaymentProvider paymentProvider;
    private final ObjectMapper objectMapper;

    @PostMapping("/{provider}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers) {

        String activeProvider = paymentProvider.providerType().name().toLowerCase();

        if (!activeProvider.equals(provider.toLowerCase())) {
            log.warn("Webhook received for provider '{}' but active provider is '{}' — ignoring",
                    provider, activeProvider);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Provider not active: " + provider);
        }

        Map<String, String> lowerHeaders = headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue,
                        (a, b) -> a));

        if (!paymentProvider.verifyWebhookSignature(payload, lowerHeaders)) {
            log.warn("[{}] Webhook signature verification failed", provider);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("event").asText("unknown");
            log.info("[{}] Webhook event received: {}", provider, eventType);

            switch (eventType) {
                case "charge.success"  -> handleChargeSuccess(event, provider);
                case "charge.failed"   -> handleChargeFailed(event, provider);
                case "refund.created"  -> handleRefundCreated(event, provider);
                // Stripe events
                case "payment_intent.succeeded"       -> handleChargeSuccess(event, provider);
                case "payment_intent.payment_failed"  -> handleChargeFailed(event, provider);
                // Flutterwave events
                case "charge.completed" -> handleChargeSuccess(event, provider);
                default -> log.info("[{}] Unhandled webhook event: {}", provider, eventType);
            }

            return ResponseEntity.ok("Webhook accepted");
        } catch (Exception e) {
            log.error("[{}] Webhook processing error: {}", provider, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }
    }

    private void handleChargeSuccess(JsonNode event, String provider) {
        JsonNode data = event.path("data");
        String reference = data.path("reference").asText(data.path("id").asText(""));
        log.info("[{}] Payment succeeded — reference={}", provider, reference);
        // TODO: update PaymentTransaction, publish PAYMENT_SUCCEEDED domain event
    }

    private void handleChargeFailed(JsonNode event, String provider) {
        JsonNode data = event.path("data");
        String reference = data.path("reference").asText(data.path("id").asText(""));
        String reason    = data.path("message").asText(data.path("failure_message").asText("unknown"));
        log.warn("[{}] Payment failed — reference={} reason={}", provider, reference, reason);
        // TODO: update PaymentTransaction, publish PAYMENT_FAILED domain event
    }

    private void handleRefundCreated(JsonNode event, String provider) {
        JsonNode data = event.path("data");
        String reference = data.path("transaction").asText(data.path("payment_intent").asText(""));
        String refundId  = data.path("id").asText("");
        log.info("[{}] Refund created — reference={} refundId={}", provider, reference, refundId);
        // TODO: update Refund entity, publish REFUND_SUCCEEDED domain event
    }
}

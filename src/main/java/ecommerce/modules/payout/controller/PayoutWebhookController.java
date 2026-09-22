package ecommerce.modules.payout.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.payout.entity.PayoutProviderEvent;
import ecommerce.modules.payout.enums.PayoutStatus;
import ecommerce.modules.payout.provider.PayoutProvider;
import ecommerce.modules.payout.repository.PayoutProviderEventRepository;
import ecommerce.modules.payout.repository.PayoutRepository;
import ecommerce.modules.payout.service.PayoutProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/payments/webhooks/payout")
@RequiredArgsConstructor
@Slf4j
public class PayoutWebhookController {

    private final PayoutProvider payoutProvider;
    private final PayoutProviderEventRepository eventRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutProcessingService payoutProcessingService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{provider}")
    public ResponseEntity<String> handlePayoutWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers) {

        String activeProvider = payoutProvider.providerType().name().toLowerCase();

        if (!activeProvider.equals(provider.toLowerCase())) {
            log.warn("Payout webhook received for provider '{}' but active provider is '{}' — ignoring",
                    sanitize(provider), activeProvider);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Provider not active");
        }

        Map<String, String> lowerHeaders = headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue,
                        (a, b) -> a));

        if (!payoutProvider.verifyWebhookSignature(payload, lowerHeaders)) {
            log.warn("[{}] Payout webhook signature verification failed", sanitize(provider));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("event").asText("unknown");
            String eventId = event.path("data").path("id").asText(
                    event.path("data").path("transfer_code").asText("unknown-" + System.currentTimeMillis()));

            log.info("[{}] Payout webhook event received: {}", sanitize(provider), sanitize(eventType));

            // Idempotency check
            if (eventRepository.existsByProviderAndEventId(provider, eventId)) {
                log.info("[{}] Duplicate payout webhook event eventId={} — ignoring",
                        sanitize(provider), sanitize(eventId));
                return ResponseEntity.ok("Already processed");
            }

            // Compute payload hash for storage
            String payloadHash = sha256Hex(payload);

            // Persist the event
            PayoutProviderEvent providerEvent = PayoutProviderEvent.builder()
                    .provider(provider)
                    .eventId(eventId)
                    .eventType(eventType)
                    .payloadHash(payloadHash)
                    .processed(false)
                    .build();
            eventRepository.save(providerEvent);

            // Handle known event types
            switch (eventType) {
                case "transfer.success" -> handleTransferSuccess(event, provider, providerEvent);
                case "transfer.failed"  -> handleTransferFailed(event, provider, providerEvent);
                case "transfer.reversed" -> handleTransferReversed(event, provider, providerEvent);
                default -> log.info("[{}] Unhandled payout webhook event: {}",
                        sanitize(provider), sanitize(eventType));
            }

            return ResponseEntity.ok("Webhook accepted");

        } catch (Exception e) {
            log.error("[{}] Payout webhook processing error: {}", sanitize(provider), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }
    }

    private void handleTransferSuccess(JsonNode event, String provider, PayoutProviderEvent providerEvent) {
        JsonNode data = event.path("data");
        String transferCode = data.path("transfer_code").asText(data.path("reference").asText(""));
        String reference = data.path("reference").asText(transferCode);

        log.info("[{}] Transfer success providerRef={}", sanitize(provider), sanitize(transferCode));

        payoutRepository.findByIdempotencyKey(reference)
                .or(() -> payoutRepository.findByProviderReference(transferCode))
                .ifPresentOrElse(payout -> {
                    if (payout.getStatus() != PayoutStatus.COMPLETED) {
                        try {
                            payoutProcessingService.processPayout(payout.getId());
                        } catch (Exception e) {
                            log.error("Failed to process payout from webhook payoutId={}: {}",
                                    payout.getId(), e.getMessage(), e);
                        }
                    }
                    providerEvent.setPayoutPublicId(payout.getPublicId());
                }, () -> log.warn("[{}] No payout found for transferCode={}", sanitize(provider), sanitize(transferCode)));

        markProcessed(providerEvent);
    }

    private void handleTransferFailed(JsonNode event, String provider, PayoutProviderEvent providerEvent) {
        JsonNode data = event.path("data");
        String transferCode = data.path("transfer_code").asText(data.path("reference").asText(""));
        String reason = data.path("reason").asText(data.path("message").asText("Transfer failed"));

        log.warn("[{}] Transfer failed providerRef={} reason={}",
                sanitize(provider), sanitize(transferCode), sanitize(reason));

        payoutRepository.findByProviderReference(transferCode)
                .ifPresentOrElse(payout -> {
                    payout.setStatus(PayoutStatus.FAILED);
                    payout.setFailureReason(reason);
                    payout.setRetryCount(payout.getRetryCount() + 1);
                    payoutRepository.save(payout);
                    providerEvent.setPayoutPublicId(payout.getPublicId());
                }, () -> log.warn("[{}] No payout found for failed transferCode={}",
                        sanitize(provider), sanitize(transferCode)));

        markProcessed(providerEvent);
    }

    private void handleTransferReversed(JsonNode event, String provider, PayoutProviderEvent providerEvent) {
        JsonNode data = event.path("data");
        String transferCode = data.path("transfer_code").asText("");

        log.warn("[{}] Transfer reversed providerRef={}", sanitize(provider), sanitize(transferCode));

        payoutRepository.findByProviderReference(transferCode)
                .ifPresentOrElse(payout -> {
                    payout.setStatus(PayoutStatus.REVERSED);
                    payoutRepository.save(payout);
                    providerEvent.setPayoutPublicId(payout.getPublicId());
                }, () -> log.warn("[{}] No payout found for reversed transferCode={}",
                        sanitize(provider), sanitize(transferCode)));

        markProcessed(providerEvent);
    }

    private void markProcessed(PayoutProviderEvent event) {
        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        eventRepository.save(event);
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "hash-error";
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}

package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.entity.ShippingWebhookEvent;
import ecommerce.modules.shipping.repository.ShippingWebhookEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/shipping/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping Webhooks", description = "Inbound carrier webhook events")
public class ShippingWebhookController {

    private final ShippingWebhookEventRepository webhookEventRepository;

    @PostMapping("/{provider}")
    @Transactional
    @Operation(summary = "Receive a shipping webhook from a carrier provider")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @PathVariable String provider,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawPayload) {

        String eventId = headers.getOrDefault("x-event-id", headers.getOrDefault("X-Event-Id", null));
        String eventType = headers.getOrDefault("x-event-type", headers.getOrDefault("X-Event-Type", "UNKNOWN"));

        if (eventId != null && webhookEventRepository.existsByProviderAndProviderEventId(provider, eventId)) {
            log.debug("Duplicate shipping webhook ignored: provider={} eventId={}", provider, eventId);
            return ResponseEntity.ok(ApiResponse.success("Already processed", null));
        }

        ShippingWebhookEvent event = ShippingWebhookEvent.builder()
                .provider(provider)
                .providerEventId(eventId != null ? eventId : java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(rawPayload)
                .build();
        webhookEventRepository.save(event);

        log.info("Received shipping webhook: provider={} eventType={}", provider, eventType);
        return ResponseEntity.ok(ApiResponse.success("Webhook received", null));
    }
}

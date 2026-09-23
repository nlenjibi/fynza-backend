package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.service.ShippingWebhookService;
import ecommerce.modules.shipping.service.WebhookSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/shipping/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping Webhooks", description = "Inbound carrier webhook events")
public class ShippingWebhookController {

    private final ShippingWebhookService webhookService;
    private final WebhookSignatureService signatureService;

    @PostMapping("/{provider}")
    @Operation(summary = "Receive a shipping webhook from a carrier provider")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @PathVariable String provider,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawPayload) {

        String signature = headers.getOrDefault("x-signature", headers.getOrDefault("X-Signature", null));
        if (!signatureService.verify(provider, rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid webhook signature"));
        }

        String eventId   = headers.getOrDefault("x-event-id",   headers.getOrDefault("X-Event-Id", null));
        String eventType = headers.getOrDefault("x-event-type",  headers.getOrDefault("X-Event-Type", "UNKNOWN"));

        boolean ingested = webhookService.ingest(provider, eventId, eventType, rawPayload);
        String message = ingested ? "Webhook received" : "Already processed";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}

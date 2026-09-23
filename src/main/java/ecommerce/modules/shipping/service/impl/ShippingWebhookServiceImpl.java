package ecommerce.modules.shipping.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.shipping.entity.ShippingWebhookEvent;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionType;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.repository.ShippingWebhookEventRepository;
import ecommerce.modules.shipping.service.ShipmentExceptionService;
import ecommerce.modules.shipping.service.ShippingWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingWebhookServiceImpl implements ShippingWebhookService {

    private final ShippingWebhookEventRepository webhookEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentExceptionService exceptionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public boolean ingest(String provider, String eventId, String eventType, String rawPayload) {
        if (eventId != null && webhookEventRepository.existsByProviderAndProviderEventId(provider, eventId)) {
            log.debug("Duplicate shipping webhook ignored: provider={} eventId={}", sanitize(provider), sanitize(eventId));
            return false;
        }

        ShippingWebhookEvent event = ShippingWebhookEvent.builder()
                .provider(provider)
                .providerEventId(eventId != null ? eventId : UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(rawPayload)
                .build();
        webhookEventRepository.save(event);
        log.info("Ingested shipping webhook: provider={} eventType={}", sanitize(provider), sanitize(eventType));

        detectAndCreateException(eventType, rawPayload);
        return true;
    }

    private void detectAndCreateException(String eventType, String rawPayload) {
        if (eventType == null) return;

        ExceptionType exType;
        ExceptionSeverity severity;
        switch (eventType.toUpperCase()) {
            case "SHIPMENT_DELIVERY_FAILED", "DELIVERY_FAILED" -> {
                exType   = ExceptionType.DELIVERY_FAILED;
                severity = ExceptionSeverity.HIGH;
            }
            case "SHIPMENT_LOST", "LOST" -> {
                exType   = ExceptionType.PACKAGE_LOST;
                severity = ExceptionSeverity.CRITICAL;
            }
            case "SHIPMENT_DAMAGED", "DAMAGED" -> {
                exType   = ExceptionType.PACKAGE_DAMAGED;
                severity = ExceptionSeverity.HIGH;
            }
            default -> { return; }
        }

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            String trackingNumber = payload.path("tracking_number").asText(
                    payload.path("trackingNumber").asText(null));
            if (trackingNumber == null || trackingNumber.isBlank()) return;

            shipmentRepository.findByTrackingNumber(trackingNumber).ifPresent(shipment ->
                    exceptionService.createException(
                            shipment.getPublicId(), exType, severity,
                            "Auto-detected from carrier webhook event: " + eventType));
        } catch (Exception e) {
            log.warn("Could not parse webhook payload for exception detection: {}", e.getMessage());
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\r', '_').replace('\n', '_');
    }
}

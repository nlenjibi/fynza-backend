package ecommerce.modules.shipping.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.shipping.entity.ShippingWebhookEvent;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionType;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.repository.ShippingWebhookEventRepository;
import ecommerce.modules.shipping.service.ShipmentExceptionService;
import ecommerce.modules.shipping.service.ShippingMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedWebhookRetryJob {

    private final ShippingWebhookEventRepository webhookEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentExceptionService exceptionService;
    private final ShippingMetricsService metricsService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${shipping.webhook-retry.cron:0 0/15 * * * *}")
    @Transactional
    public void retryFailedWebhooks() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<ShippingWebhookEvent> pending = webhookEventRepository.findByProcessedFalseAndCreatedAtBefore(cutoff);

        if (pending.isEmpty()) {
            log.debug("FailedWebhookRetryJob: no pending webhook events to retry");
            return;
        }

        log.info("FailedWebhookRetryJob: retrying {} unprocessed webhook events", pending.size());
        int retried = 0;

        for (ShippingWebhookEvent event : pending) {
            try {
                reprocessEvent(event);
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                event.setErrorMessage(null);
                webhookEventRepository.save(event);
                metricsService.recordWebhookRetried();
                retried++;
            } catch (Exception e) {
                log.warn("FailedWebhookRetryJob: retry failed for event={}: {}", event.getPublicId(), e.getMessage());
                event.setErrorMessage("Retry failed: " + e.getMessage());
                webhookEventRepository.save(event);
            }
        }

        log.info("FailedWebhookRetryJob: completed — retried={} of total={}", retried, pending.size());
    }

    private void reprocessEvent(ShippingWebhookEvent event) throws Exception {
        String eventType = event.getEventType();
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

        JsonNode payload = objectMapper.readTree(event.getPayload());
        String trackingNumber = payload.path("tracking_number").asText(
                payload.path("trackingNumber").asText(null));
        if (trackingNumber == null || trackingNumber.isBlank()) return;

        shipmentRepository.findByTrackingNumber(trackingNumber).ifPresent(shipment ->
                exceptionService.createException(
                        shipment.getPublicId(), exType, severity,
                        "Retried auto-detection from carrier webhook: " + eventType));
    }
}

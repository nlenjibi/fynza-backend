package ecommerce.modules.shipping.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.entity.ShippingWebhookEvent;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionType;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.repository.ShippingWebhookEventRepository;
import ecommerce.modules.shipping.service.ShipmentExceptionService;
import ecommerce.modules.shipping.service.ShippingMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingWebhookServiceImplTest {

    @Mock private ShippingWebhookEventRepository webhookEventRepository;
    @Mock private ShipmentRepository             shipmentRepository;
    @Mock private ShipmentExceptionService       exceptionService;
    @Mock private ShippingMetricsService         metricsService;

    @InjectMocks
    private ShippingWebhookServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void injectObjectMapper() throws Exception {
        var field = ShippingWebhookServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(service, objectMapper);
    }

    private static final String PROVIDER = "dhl";
    private static final String EVENT_ID = "evt-001";

    private ShippingWebhookEvent stubSavedEvent(String eventType, String payload) {
        return ShippingWebhookEvent.builder()
                .provider(PROVIDER)
                .providerEventId(EVENT_ID)
                .eventType(eventType)
                .payload(payload)
                .processed(false)
                .build();
    }

    @Nested
    class DuplicateDetection {

        @Test
        void duplicateEventId_returnsFalseAndDoesNotSave() {
            when(webhookEventRepository.existsByProviderAndProviderEventId(PROVIDER, EVENT_ID))
                    .thenReturn(true);

            boolean result = service.ingest(PROVIDER, EVENT_ID, "SHIPMENT_UPDATE", "{}");

            assertThat(result).isFalse();
            verify(webhookEventRepository, never()).save(any());
            verify(metricsService, never()).recordWebhookReceived();
        }
    }

    @Nested
    class NewEvent {

        @Test
        void unknownEventType_savedAndMarkedProcessedTrue() {
            when(webhookEventRepository.existsByProviderAndProviderEventId(PROVIDER, EVENT_ID))
                    .thenReturn(false);
            when(webhookEventRepository.save(any())).thenReturn(stubSavedEvent("SHIPMENT_UPDATE", "{}"));

            boolean result = service.ingest(PROVIDER, EVENT_ID, "SHIPMENT_UPDATE", "{}");

            assertThat(result).isTrue();
            verify(metricsService).recordWebhookReceived();

            ArgumentCaptor<ShippingWebhookEvent> captor = ArgumentCaptor.forClass(ShippingWebhookEvent.class);
            verify(webhookEventRepository, times(2)).save(captor.capture());

            ShippingWebhookEvent finalSave = captor.getAllValues().get(1);
            assertThat(finalSave.getProcessed()).isTrue();
            assertThat(finalSave.getProcessedAt()).isNotNull();
        }

        @Test
        void nullEventId_generatesUuidAndSaves() {
            when(webhookEventRepository.existsByProviderAndProviderEventId(any(), any()))
                    .thenReturn(false);

            ShippingWebhookEvent savedEvent = ShippingWebhookEvent.builder()
                    .provider(PROVIDER)
                    .providerEventId(UUID.randomUUID().toString())
                    .eventType("SHIPMENT_UPDATE")
                    .payload("{}")
                    .processed(false)
                    .build();
            when(webhookEventRepository.save(any())).thenReturn(savedEvent);

            boolean result = service.ingest(PROVIDER, null, "SHIPMENT_UPDATE", "{}");

            assertThat(result).isTrue();
            ArgumentCaptor<ShippingWebhookEvent> captor = ArgumentCaptor.forClass(ShippingWebhookEvent.class);
            verify(webhookEventRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getProviderEventId()).isNotBlank();
        }
    }

    @Nested
    class ExceptionAutoDetection {

        @Test
        void deliveryFailedEvent_triggersExceptionCreationForMatchedShipment() throws Exception {
            String trackingNumber = "TRK-9999";
            String payload = objectMapper.writeValueAsString(Map.of("tracking_number", trackingNumber));

            UUID shipmentId = UUID.randomUUID();
            Shipment shipment = Shipment.builder().shipmentNumber("SHP-2026-009999").build();
            var pubIdField = Shipment.class.getDeclaredField("publicId");
            pubIdField.setAccessible(true);
            pubIdField.set(shipment, shipmentId);

            when(webhookEventRepository.existsByProviderAndProviderEventId(PROVIDER, EVENT_ID)).thenReturn(false);
            when(webhookEventRepository.save(any())).thenReturn(stubSavedEvent("DELIVERY_FAILED", payload));
            when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(shipment));

            service.ingest(PROVIDER, EVENT_ID, "DELIVERY_FAILED", payload);

            verify(exceptionService).createException(
                    eq(shipmentId),
                    eq(ExceptionType.DELIVERY_FAILED),
                    eq(ExceptionSeverity.HIGH),
                    contains("DELIVERY_FAILED"));
        }

        @Test
        void lostEvent_triggersExceptionWithCriticalSeverity() throws Exception {
            String trackingNumber = "TRK-LOST-1";
            String payload = objectMapper.writeValueAsString(Map.of("tracking_number", trackingNumber));

            UUID shipmentId = UUID.randomUUID();
            Shipment shipment = Shipment.builder().shipmentNumber("SHP-2026-000001").build();
            var pubIdField = Shipment.class.getDeclaredField("publicId");
            pubIdField.setAccessible(true);
            pubIdField.set(shipment, shipmentId);

            when(webhookEventRepository.existsByProviderAndProviderEventId(PROVIDER, EVENT_ID)).thenReturn(false);
            when(webhookEventRepository.save(any())).thenReturn(stubSavedEvent("LOST", payload));
            when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(shipment));

            service.ingest(PROVIDER, EVENT_ID, "LOST", payload);

            verify(exceptionService).createException(
                    eq(shipmentId), eq(ExceptionType.PACKAGE_LOST), eq(ExceptionSeverity.CRITICAL), any());
        }

        @Test
        void deliveryFailedEvent_missingTrackingNumber_noExceptionCreated() {
            when(webhookEventRepository.existsByProviderAndProviderEventId(PROVIDER, EVENT_ID)).thenReturn(false);
            when(webhookEventRepository.save(any())).thenReturn(stubSavedEvent("DELIVERY_FAILED", "{}"));

            service.ingest(PROVIDER, EVENT_ID, "DELIVERY_FAILED", "{}");

            verify(exceptionService, never()).createException(any(), any(ExceptionType.class),
                    any(ExceptionSeverity.class), any());
        }

        @Test
        void malformedJson_isSwallowedGracefully_andEventMarkedProcessed() {
            // JSON parse error in detectAndCreateException is caught internally;
            // the outer try in ingest() still completes, so processed=true.
            String malformed = "NOT_JSON{{{{";
            when(webhookEventRepository.existsByProviderAndProviderEventId(PROVIDER, EVENT_ID)).thenReturn(false);
            when(webhookEventRepository.save(any())).thenReturn(stubSavedEvent("DELIVERY_FAILED", malformed));

            boolean result = service.ingest(PROVIDER, EVENT_ID, "DELIVERY_FAILED", malformed);

            assertThat(result).isTrue();

            ArgumentCaptor<ShippingWebhookEvent> captor = ArgumentCaptor.forClass(ShippingWebhookEvent.class);
            verify(webhookEventRepository, times(2)).save(captor.capture());

            ShippingWebhookEvent finalSave = captor.getAllValues().get(1);
            assertThat(finalSave.getProcessed()).isTrue();
            verify(exceptionService, never()).createException(any(), any(ExceptionType.class),
                    any(ExceptionSeverity.class), any());
        }
    }
}

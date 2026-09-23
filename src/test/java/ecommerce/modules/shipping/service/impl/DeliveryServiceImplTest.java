package ecommerce.modules.shipping.service.impl;

import ecommerce.modules.shipping.dto.request.DeliveryAttemptRequest;
import ecommerce.modules.shipping.dto.response.DeliveryAttemptResponse;
import ecommerce.modules.shipping.entity.DeliveryAttempt;
import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.repository.DeliveryAttemptRepository;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock private DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock private ShipmentRepository        shipmentRepository;

    @InjectMocks
    private DeliveryServiceImpl service;

    private UUID shipmentPublicId;
    private Shipment shipment;

    @BeforeEach
    void setUp() throws Exception {
        shipmentPublicId = UUID.randomUUID();
        shipment = Shipment.builder().shipmentNumber("SHP-2026-000042").build();

        var pubIdField = Shipment.class.getDeclaredField("publicId");
        pubIdField.setAccessible(true);
        pubIdField.set(shipment, shipmentPublicId);
    }

    private DeliveryAttemptRequest failedAttemptRequest() {
        DeliveryAttemptRequest req = new DeliveryAttemptRequest();
        req.setStatus("FAILED");
        req.setFailureReason("Customer not home");
        req.setLocation("Accra Central");
        return req;
    }

    private DeliveryAttempt attemptEntity(int attemptNumber) throws Exception {
        DeliveryAttempt a = DeliveryAttempt.builder()
                .shipmentId(shipmentPublicId)
                .attemptNumber(attemptNumber)
                .status("FAILED")
                .failureReason("Customer not home")
                .location("Accra Central")
                .build();
        var pubIdField = DeliveryAttempt.class.getDeclaredField("publicId");
        pubIdField.setAccessible(true);
        pubIdField.set(a, UUID.randomUUID());
        var createdAtField = DeliveryAttempt.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(a, Instant.now());
        return a;
    }

    @Nested
    class RecordAttempt {

        @Test
        void firstAttempt_setsAttemptNumberToOne() throws Exception {
            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(deliveryAttemptRepository.countByShipmentId(shipmentPublicId)).thenReturn(0);
            when(deliveryAttemptRepository.save(any())).thenReturn(attemptEntity(1));

            DeliveryAttemptResponse response = service.recordAttempt(shipmentPublicId, failedAttemptRequest());

            assertThat(response.getAttemptNumber()).isEqualTo(1);

            ArgumentCaptor<DeliveryAttempt> captor = ArgumentCaptor.forClass(DeliveryAttempt.class);
            verify(deliveryAttemptRepository).save(captor.capture());
            assertThat(captor.getValue().getAttemptNumber()).isEqualTo(1);
        }

        @Test
        void secondAttempt_incrementsToTwo() throws Exception {
            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(deliveryAttemptRepository.countByShipmentId(shipmentPublicId)).thenReturn(1);
            when(deliveryAttemptRepository.save(any())).thenReturn(attemptEntity(2));

            DeliveryAttemptResponse response = service.recordAttempt(shipmentPublicId, failedAttemptRequest());

            assertThat(response.getAttemptNumber()).isEqualTo(2);
        }

        @Test
        void shipmentNotFound_throwsShipmentNotFoundException() {
            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recordAttempt(shipmentPublicId, failedAttemptRequest()))
                    .isInstanceOf(ShipmentNotFoundException.class)
                    .hasMessageContaining(shipmentPublicId.toString());

            verify(deliveryAttemptRepository, never()).save(any());
        }
    }

    @Nested
    class AutoTransitionToReturnToSender {

        @Test
        void thirdFailedAttempt_withDeliveryFailedStatus_transitionsShipmentToReturnToSender() throws Exception {
            shipment.setStatus(ShipmentStatus.DELIVERY_FAILED);

            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(deliveryAttemptRepository.countByShipmentId(shipmentPublicId)).thenReturn(2);
            when(deliveryAttemptRepository.save(any())).thenReturn(attemptEntity(3));

            service.recordAttempt(shipmentPublicId, failedAttemptRequest());

            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.RETURN_TO_SENDER);
            verify(shipmentRepository).save(shipment);
        }

        @Test
        void thirdAttempt_withNonDeliveryFailedStatus_doesNotAutoTransition() throws Exception {
            shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);

            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(deliveryAttemptRepository.countByShipmentId(shipmentPublicId)).thenReturn(2);
            when(deliveryAttemptRepository.save(any())).thenReturn(attemptEntity(3));

            service.recordAttempt(shipmentPublicId, failedAttemptRequest());

            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
            verify(shipmentRepository, never()).save(shipment);
        }

        @Test
        void secondAttempt_withDeliveryFailedStatus_doesNotAutoTransition() throws Exception {
            shipment.setStatus(ShipmentStatus.DELIVERY_FAILED);

            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(deliveryAttemptRepository.countByShipmentId(shipmentPublicId)).thenReturn(1);
            when(deliveryAttemptRepository.save(any())).thenReturn(attemptEntity(2));

            service.recordAttempt(shipmentPublicId, failedAttemptRequest());

            assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERY_FAILED);
            verify(shipmentRepository, never()).save(shipment);
        }
    }
}

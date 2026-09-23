package ecommerce.modules.shipping.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.shipping.dto.request.ResolveExceptionRequest;
import ecommerce.modules.shipping.dto.request.ShipmentExceptionRequest;
import ecommerce.modules.shipping.dto.response.ShipmentExceptionResponse;
import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.entity.ShipmentException;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionStatus;
import ecommerce.modules.shipping.enums.ExceptionType;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.repository.ShipmentExceptionRepository;
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
class ShipmentExceptionServiceImplTest {

    @Mock private ShipmentExceptionRepository exceptionRepository;
    @Mock private ShipmentRepository          shipmentRepository;

    @InjectMocks
    private ShipmentExceptionServiceImpl service;

    private UUID shipmentPublicId;
    private UUID exceptionPublicId;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipmentPublicId  = UUID.randomUUID();
        exceptionPublicId = UUID.randomUUID();
        shipment = Shipment.builder().shipmentNumber("SHP-2026-000001").build();
    }

    @Nested
    class CreateExceptionViaRequest {

        @Test
        void happyPath_persistsOpenExceptionAndReturnsResponse() {
            ShipmentExceptionRequest request = new ShipmentExceptionRequest();
            request.setType(ExceptionType.DELIVERY_FAILED);
            request.setSeverity(ExceptionSeverity.HIGH);
            request.setDescription("Door code not working");

            ShipmentException saved = ShipmentException.builder()
                    .publicId(exceptionPublicId)
                    .shipmentId(shipmentPublicId)
                    .type(ExceptionType.DELIVERY_FAILED)
                    .severity(ExceptionSeverity.HIGH)
                    .description("Door code not working")
                    .status(ExceptionStatus.OPEN)
                    .detectedAt(Instant.now())
                    .build();

            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(exceptionRepository.save(any(ShipmentException.class))).thenReturn(saved);

            ShipmentExceptionResponse response = service.createException(shipmentPublicId, request);

            assertThat(response.getType()).isEqualTo(ExceptionType.DELIVERY_FAILED);
            assertThat(response.getSeverity()).isEqualTo(ExceptionSeverity.HIGH);
            assertThat(response.getStatus()).isEqualTo(ExceptionStatus.OPEN);
            assertThat(response.getShipmentId()).isEqualTo(shipmentPublicId);

            ArgumentCaptor<ShipmentException> captor = ArgumentCaptor.forClass(ShipmentException.class);
            verify(exceptionRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ExceptionStatus.OPEN);
            assertThat(captor.getValue().getDetectedAt()).isNotNull();
        }

        @Test
        void shipmentNotFound_throwsShipmentNotFoundException_andNothingIsPersisted() {
            ShipmentExceptionRequest request = new ShipmentExceptionRequest();
            request.setType(ExceptionType.PACKAGE_LOST);
            request.setSeverity(ExceptionSeverity.CRITICAL);

            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createException(shipmentPublicId, request))
                    .isInstanceOf(ShipmentNotFoundException.class)
                    .hasMessageContaining(shipmentPublicId.toString());

            verify(exceptionRepository, never()).save(any());
        }
    }

    @Nested
    class CreateExceptionDirectOverload {

        @Test
        void happyPath_delegatesToRepositoryAndReturnsResponse() {
            ShipmentException saved = ShipmentException.builder()
                    .publicId(exceptionPublicId)
                    .shipmentId(shipmentPublicId)
                    .type(ExceptionType.PACKAGE_LOST)
                    .severity(ExceptionSeverity.CRITICAL)
                    .description("Lost in transit")
                    .status(ExceptionStatus.OPEN)
                    .detectedAt(Instant.now())
                    .build();

            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.of(shipment));
            when(exceptionRepository.save(any(ShipmentException.class))).thenReturn(saved);

            ShipmentExceptionResponse response = service.createException(
                    shipmentPublicId, ExceptionType.PACKAGE_LOST, ExceptionSeverity.CRITICAL, "Lost in transit");

            assertThat(response.getType()).isEqualTo(ExceptionType.PACKAGE_LOST);
            assertThat(response.getSeverity()).isEqualTo(ExceptionSeverity.CRITICAL);
        }

        @Test
        void shipmentNotFound_throwsShipmentNotFoundException() {
            when(shipmentRepository.findByPublicId(shipmentPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createException(
                    shipmentPublicId, ExceptionType.OTHER, ExceptionSeverity.LOW, "desc"))
                    .isInstanceOf(ShipmentNotFoundException.class);
        }
    }

    @Nested
    class ResolveException {

        @Test
        void happyPath_setsResolvedStatusTimestampAndPersists() {
            ShipmentException existing = ShipmentException.builder()
                    .publicId(exceptionPublicId)
                    .shipmentId(shipmentPublicId)
                    .type(ExceptionType.CARRIER_DELAY)
                    .severity(ExceptionSeverity.MEDIUM)
                    .status(ExceptionStatus.OPEN)
                    .detectedAt(Instant.now())
                    .build();

            ShipmentException afterSave = ShipmentException.builder()
                    .publicId(exceptionPublicId)
                    .shipmentId(shipmentPublicId)
                    .type(ExceptionType.CARRIER_DELAY)
                    .severity(ExceptionSeverity.MEDIUM)
                    .status(ExceptionStatus.RESOLVED)
                    .resolvedBy("admin@fynza.com")
                    .resolutionNotes("Carrier confirmed delivery rescheduled")
                    .resolvedAt(Instant.now())
                    .detectedAt(Instant.now())
                    .build();

            ResolveExceptionRequest request = new ResolveExceptionRequest();
            request.setResolvedBy("admin@fynza.com");
            request.setResolutionNotes("Carrier confirmed delivery rescheduled");

            when(exceptionRepository.findByPublicId(exceptionPublicId)).thenReturn(Optional.of(existing));
            when(exceptionRepository.save(existing)).thenReturn(afterSave);

            ShipmentExceptionResponse response = service.resolveException(exceptionPublicId, request);

            assertThat(response.getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
            assertThat(response.getResolvedBy()).isEqualTo("admin@fynza.com");
            assertThat(response.getResolutionNotes()).isEqualTo("Carrier confirmed delivery rescheduled");

            ArgumentCaptor<ShipmentException> captor = ArgumentCaptor.forClass(ShipmentException.class);
            verify(exceptionRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ExceptionStatus.RESOLVED);
            assertThat(captor.getValue().getResolvedAt()).isNotNull();
            assertThat(captor.getValue().getResolvedBy()).isEqualTo("admin@fynza.com");
        }

        @Test
        void exceptionNotFound_throwsResourceNotFoundException() {
            when(exceptionRepository.findByPublicId(exceptionPublicId)).thenReturn(Optional.empty());

            ResolveExceptionRequest request = new ResolveExceptionRequest();
            request.setResolvedBy("admin@fynza.com");

            assertThatThrownBy(() -> service.resolveException(exceptionPublicId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(exceptionPublicId.toString());
        }
    }
}

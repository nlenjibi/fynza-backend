package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.ReserveStockRequest;
import ecommerce.modules.inventory.dto.response.InventoryReservationResponse;
import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.entity.InventoryReservation;
import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.InventoryReservationStatus;
import ecommerce.modules.inventory.exception.InsufficientStockException;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.InventoryReservationRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.impl.InventoryReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryReservationServiceImpl")
class InventoryReservationServiceImplTest {

    @Mock private InventoryRepository            inventoryRepository;
    @Mock private InventoryReservationRepository reservationRepository;
    @Mock private StockMovementRepository        movementRepository;
    @Mock private ApplicationEventPublisher      eventPublisher;

    @InjectMocks
    private InventoryReservationServiceImpl service;

    private UUID inventoryPublicId;
    private UUID reservationPublicId;
    private Inventory inventory;
    private InventoryReservation reservation;

    @BeforeEach
    void setUp() {
        inventoryPublicId   = UUID.randomUUID();
        reservationPublicId = UUID.randomUUID();

        inventory = Inventory.builder()
                .onHandQuantity(100)
                .reservedQuantity(10)
                .allowBackorder(false)
                .build();
        setField(inventory, "id", 200L);
        setField(inventory, "publicId", inventoryPublicId);
        setField(inventory, "isActive", true);

        reservation = InventoryReservation.builder()
                .inventoryId(200L)
                .orderId(UUID.randomUUID())
                .quantity(5)
                .status(InventoryReservationStatus.ACTIVE)
                .build();
        setField(reservation, "publicId", reservationPublicId);
        setField(reservation, "id", 50L);
    }

    // ─── reserve ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("happy path — atomic reserve succeeds, reservation created, event published")
        void reserve_success_createsReservationAndPublishesEvent() {
            ReserveStockRequest request = new ReserveStockRequest();
            request.setQuantity(10);
            request.setOrderId(UUID.randomUUID());
            request.setExpiresAt(Instant.now().plusSeconds(3600));

            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicReserve(eq(200L), eq(10), any(Instant.class))).thenReturn(1);
            when(reservationRepository.save(any(InventoryReservation.class))).thenReturn(reservation);
            when(movementRepository.save(any(StockMovement.class))).thenReturn(StockMovement.builder().build());

            InventoryReservationResponse result = service.reserve(inventoryPublicId, request);

            assertThat(result).isNotNull();
            verify(inventoryRepository).atomicReserve(eq(200L), eq(10), any(Instant.class));
            verify(reservationRepository).save(any(InventoryReservation.class));
            verify(movementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(ecommerce.modules.inventory.event.StockReservedEvent.class));
        }

        @Test
        @DisplayName("throws InsufficientStockException when atomic reserve returns 0")
        void reserve_insufficientStock_throwsInsufficientStockException() {
            ReserveStockRequest request = new ReserveStockRequest();
            request.setQuantity(999);

            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicReserve(eq(200L), eq(999), any(Instant.class))).thenReturn(0);

            assertThatThrownBy(() -> service.reserve(inventoryPublicId, request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");

            verify(reservationRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when inventory public ID does not exist")
        void reserve_inventoryNotFound_throwsInventoryNotFoundException() {
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reserve(inventoryPublicId, new ReserveStockRequest()))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining(inventoryPublicId.toString());

            verify(reservationRepository, never()).save(any());
        }
    }

    // ─── confirm ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("happy path — ACTIVE reservation confirmed, movement saved, status becomes CONFIRMED")
        void confirm_activeReservation_statusBecomesConfirmed() {
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));
            when(inventoryRepository.findById(200L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicCommit(eq(200L), eq(5), any(Instant.class))).thenReturn(1);
            when(movementRepository.save(any(StockMovement.class))).thenReturn(StockMovement.builder().build());
            when(reservationRepository.save(reservation)).thenReturn(reservation);

            InventoryReservationResponse result = service.confirm(reservationPublicId);

            assertThat(result).isNotNull();
            assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.CONFIRMED);
            verify(inventoryRepository).atomicCommit(eq(200L), eq(5), any(Instant.class));
            verify(movementRepository).save(any(StockMovement.class));
            verify(reservationRepository).save(reservation);
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when reservation is already CONFIRMED")
        void confirm_alreadyConfirmed_throwsInvalidInventoryOperationException() {
            reservation.setStatus(InventoryReservationStatus.CONFIRMED);
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.confirm(reservationPublicId))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("not ACTIVE");

            verify(inventoryRepository, never()).atomicCommit(any(), anyInt(), any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when reservation is RELEASED")
        void confirm_releasedReservation_throwsInvalidInventoryOperationException() {
            reservation.setStatus(InventoryReservationStatus.RELEASED);
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.confirm(reservationPublicId))
                    .isInstanceOf(InvalidInventoryOperationException.class);

            verify(inventoryRepository, never()).atomicCommit(any(), anyInt(), any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when atomicCommit returns 0")
        void confirm_atomicCommitFails_throwsInvalidInventoryOperationException() {
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));
            when(inventoryRepository.findById(200L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicCommit(eq(200L), eq(5), any(Instant.class))).thenReturn(0);

            assertThatThrownBy(() -> service.confirm(reservationPublicId))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("Cannot confirm reservation");

            verify(reservationRepository, never()).save(reservation);
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when reservation does not exist")
        void confirm_reservationNotFound_throwsInventoryNotFoundException() {
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirm(reservationPublicId))
                    .isInstanceOf(InventoryNotFoundException.class);
        }
    }

    // ─── release ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("happy path — ACTIVE reservation released, atomic release called, event published")
        void release_activeReservation_releasesAndPublishesEvent() {
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));
            when(inventoryRepository.findById(200L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicRelease(eq(200L), eq(5), any(Instant.class))).thenReturn(1);
            when(movementRepository.save(any(StockMovement.class))).thenReturn(StockMovement.builder().build());
            when(reservationRepository.save(reservation)).thenReturn(reservation);

            InventoryReservationResponse result = service.release(reservationPublicId);

            assertThat(result).isNotNull();
            assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
            assertThat(reservation.getReleasedAt()).isNotNull();
            verify(inventoryRepository).atomicRelease(eq(200L), eq(5), any(Instant.class));
            verify(movementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any(ecommerce.modules.inventory.event.StockReleasedEvent.class));
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when reservation is already RELEASED")
        void release_alreadyReleased_throwsInvalidInventoryOperationException() {
            reservation.setStatus(InventoryReservationStatus.RELEASED);
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.release(reservationPublicId))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("already");

            verify(inventoryRepository, never()).atomicRelease(any(), anyInt(), any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when reservation is EXPIRED")
        void release_expiredReservation_throwsInvalidInventoryOperationException() {
            reservation.setStatus(InventoryReservationStatus.EXPIRED);
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> service.release(reservationPublicId))
                    .isInstanceOf(InvalidInventoryOperationException.class);

            verify(inventoryRepository, never()).atomicRelease(any(), anyInt(), any());
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when reservation does not exist")
        void release_reservationNotFound_throwsInventoryNotFoundException() {
            when(reservationRepository.findByPublicId(reservationPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.release(reservationPublicId))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining(reservationPublicId.toString());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

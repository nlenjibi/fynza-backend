package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.CreateTransferRequest;
import ecommerce.modules.inventory.dto.request.ReceiveTransferRequest;
import ecommerce.modules.inventory.dto.response.InventoryTransferResponse;
import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.entity.InventoryTransfer;
import ecommerce.modules.inventory.entity.InventoryTransferItem;
import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.TransferStatus;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.InventoryTransferItemRepository;
import ecommerce.modules.inventory.repository.InventoryTransferRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.impl.InventoryTransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventoryTransferServiceImpl")
class InventoryTransferServiceImplTest {

    @Mock private InventoryTransferRepository     transferRepository;
    @Mock private InventoryTransferItemRepository transferItemRepository;
    @Mock private InventoryRepository             inventoryRepository;
    @Mock private StockMovementRepository         movementRepository;

    @InjectMocks
    private InventoryTransferServiceImpl service;

    private UUID requestedBy;
    private UUID transferPublicId;
    private InventoryTransfer transfer;
    private Inventory sourceInventory;

    @BeforeEach
    void setUp() {
        requestedBy      = UUID.randomUUID();
        transferPublicId = UUID.randomUUID();

        transfer = InventoryTransfer.builder()
                .sourceLocationId(1L)
                .destinationLocationId(2L)
                .requestedBy(requestedBy)
                .status(TransferStatus.REQUESTED)
                .build();
        setField(transfer, "id", 10L);
        setField(transfer, "publicId", transferPublicId);

        sourceInventory = Inventory.builder()
                .productId(UUID.randomUUID())
                .sellerId(1L)
                .locationId(1L)
                .onHandQuantity(100)
                .reservedQuantity(0)
                .lowStockThreshold(5)
                .allowBackorder(false)
                .build();
        setField(sourceInventory, "id", 200L);
        setField(sourceInventory, "publicId", UUID.randomUUID());
        setField(sourceInventory, "isActive", true);
    }

    // ─── requestTransfer (mapped to createTransfer) ───────────────────────────

    @Nested
    @DisplayName("requestTransfer")
    class RequestTransfer {

        @Test
        @DisplayName("happy path — transfer and items saved, status is REQUESTED")
        void requestTransfer_happyPath_createsTransferWithItems() {
            UUID inventoryPublicId = UUID.randomUUID();
            setField(sourceInventory, "publicId", inventoryPublicId);

            CreateTransferRequest.TransferItemRequest itemRequest = new CreateTransferRequest.TransferItemRequest();
            itemRequest.setInventoryPublicId(inventoryPublicId);
            itemRequest.setQuantity(10);

            CreateTransferRequest request = new CreateTransferRequest();
            request.setSourceLocationId(1L);
            request.setDestinationLocationId(2L);
            request.setItems(List.of(itemRequest));

            when(transferRepository.save(any(InventoryTransfer.class))).thenReturn(transfer);
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(sourceInventory));
            when(transferItemRepository.save(any(InventoryTransferItem.class)))
                    .thenReturn(InventoryTransferItem.builder().build());

            InventoryTransferResponse result = service.requestTransfer(request, requestedBy);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransferStatus.REQUESTED);
            verify(transferRepository).save(any(InventoryTransfer.class));
            verify(transferItemRepository).save(any(InventoryTransferItem.class));
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when source and destination are the same location")
        void requestTransfer_sameLocations_throwsInvalidInventoryOperationException() {
            CreateTransferRequest request = new CreateTransferRequest();
            request.setSourceLocationId(1L);
            request.setDestinationLocationId(1L);
            request.setItems(List.of());

            assertThatThrownBy(() -> service.requestTransfer(request, requestedBy))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("must be different");

            verify(transferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when referenced inventory item does not exist")
        void requestTransfer_inventoryItemNotFound_throwsInventoryNotFoundException() {
            UUID missingInventoryId = UUID.randomUUID();

            CreateTransferRequest.TransferItemRequest itemRequest = new CreateTransferRequest.TransferItemRequest();
            itemRequest.setInventoryPublicId(missingInventoryId);
            itemRequest.setQuantity(5);

            CreateTransferRequest request = new CreateTransferRequest();
            request.setSourceLocationId(1L);
            request.setDestinationLocationId(2L);
            request.setItems(List.of(itemRequest));

            when(transferRepository.save(any(InventoryTransfer.class))).thenReturn(transfer);
            when(inventoryRepository.findByPublicId(missingInventoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requestTransfer(request, requestedBy))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining(missingInventoryId.toString());
        }

        @Test
        @DisplayName("saves a transfer item for each item in the request list")
        void requestTransfer_multipleItems_savesAllTransferItems() {
            UUID inventoryId1 = UUID.randomUUID();
            UUID inventoryId2 = UUID.randomUUID();

            Inventory inventory2 = Inventory.builder()
                    .productId(UUID.randomUUID()).sellerId(1L).locationId(1L)
                    .onHandQuantity(50).build();
            setField(inventory2, "id", 201L);
            setField(sourceInventory, "publicId", inventoryId1);
            setField(inventory2, "publicId", inventoryId2);

            CreateTransferRequest.TransferItemRequest item1 = new CreateTransferRequest.TransferItemRequest();
            item1.setInventoryPublicId(inventoryId1);
            item1.setQuantity(5);

            CreateTransferRequest.TransferItemRequest item2 = new CreateTransferRequest.TransferItemRequest();
            item2.setInventoryPublicId(inventoryId2);
            item2.setQuantity(15);

            CreateTransferRequest request = new CreateTransferRequest();
            request.setSourceLocationId(1L);
            request.setDestinationLocationId(2L);
            request.setItems(List.of(item1, item2));

            when(transferRepository.save(any(InventoryTransfer.class))).thenReturn(transfer);
            when(inventoryRepository.findByPublicId(inventoryId1)).thenReturn(Optional.of(sourceInventory));
            when(inventoryRepository.findByPublicId(inventoryId2)).thenReturn(Optional.of(inventory2));
            when(transferItemRepository.save(any(InventoryTransferItem.class)))
                    .thenReturn(InventoryTransferItem.builder().build());

            service.requestTransfer(request, requestedBy);

            verify(transferItemRepository, times(2)).save(any(InventoryTransferItem.class));
        }
    }

    // ─── approveTransfer ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveTransfer")
    class ApproveTransfer {

        @Test
        @DisplayName("happy path — REQUESTED transfer transitions to APPROVED with approvedBy set")
        void approveTransfer_requestedTransfer_statusBecomesApproved() {
            UUID approverId = UUID.randomUUID();
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));
            when(transferRepository.save(transfer)).thenReturn(transfer);

            InventoryTransferResponse result = service.approveTransfer(transferPublicId, approverId);

            assertThat(result).isNotNull();
            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.APPROVED);
            assertThat(transfer.getApprovedBy()).isEqualTo(approverId);
            verify(transferRepository).save(transfer);
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when transfer is already APPROVED")
        void approveTransfer_alreadyApproved_throwsInvalidInventoryOperationException() {
            transfer.setStatus(TransferStatus.APPROVED);
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));

            assertThatThrownBy(() -> service.approveTransfer(transferPublicId, UUID.randomUUID()))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("REQUESTED")
                    .hasMessageContaining("APPROVED");

            verify(transferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when transfer is RECEIVED")
        void approveTransfer_receivedTransfer_throwsInvalidInventoryOperationException() {
            transfer.setStatus(TransferStatus.RECEIVED);
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));

            assertThatThrownBy(() -> service.approveTransfer(transferPublicId, UUID.randomUUID()))
                    .isInstanceOf(InvalidInventoryOperationException.class);

            verify(transferRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when transfer does not exist")
        void approveTransfer_notFound_throwsInventoryNotFoundException() {
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveTransfer(transferPublicId, UUID.randomUUID()))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining(transferPublicId.toString());
        }
    }

    // ─── receiveTransfer ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("receiveTransfer")
    class ReceiveTransfer {

        @Test
        @DisplayName("happy path — APPROVED transfer received, stock deducted from source and added to destination")
        void receiveTransfer_approvedTransfer_adjustsBothLocations() {
            transfer.setStatus(TransferStatus.APPROVED);

            InventoryTransferItem transferItem = InventoryTransferItem.builder()
                    .transferId(10L)
                    .inventoryId(200L)
                    .quantity(20)
                    .build();
            setField(transferItem, "id", 30L);

            Inventory destInventory = Inventory.builder()
                    .productId(sourceInventory.getProductId())
                    .sellerId(1L)
                    .locationId(2L)
                    .onHandQuantity(0)
                    .build();
            setField(destInventory, "id", 201L);

            ReceiveTransferRequest.ReceivedItemRequest receivedItem = new ReceiveTransferRequest.ReceivedItemRequest();
            receivedItem.setTransferItemId(30L);
            receivedItem.setReceivedQuantity(20);

            ReceiveTransferRequest request = new ReceiveTransferRequest();
            request.setItems(List.of(receivedItem));

            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));
            when(transferItemRepository.findByTransferId(10L)).thenReturn(List.of(transferItem));
            when(inventoryRepository.findById(200L)).thenReturn(Optional.of(sourceInventory));
            when(inventoryRepository.atomicAdjust(eq(200L), eq(-20), any(Instant.class))).thenReturn(1);
            when(inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                    sourceInventory.getProductId(), 2L)).thenReturn(Optional.of(destInventory));
            when(inventoryRepository.atomicAdjust(eq(201L), eq(20), any(Instant.class))).thenReturn(1);
            when(movementRepository.save(any(StockMovement.class))).thenReturn(StockMovement.builder().build());
            when(transferItemRepository.save(transferItem)).thenReturn(transferItem);
            when(transferRepository.save(transfer)).thenReturn(transfer);

            InventoryTransferResponse result = service.receiveTransfer(transferPublicId, request, UUID.randomUUID());

            assertThat(result).isNotNull();
            assertThat(transfer.getStatus()).isEqualTo(TransferStatus.RECEIVED);
            assertThat(transfer.getCompletedAt()).isNotNull();
            // TRANSFER_OUT from source, TRANSFER_IN to destination
            verify(inventoryRepository).atomicAdjust(eq(200L), eq(-20), any(Instant.class));
            verify(inventoryRepository).atomicAdjust(eq(201L), eq(20), any(Instant.class));
            verify(movementRepository, times(2)).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("creates destination inventory when none exists at destination location")
        void receiveTransfer_noDestinationInventory_createsNewInventory() {
            transfer.setStatus(TransferStatus.APPROVED);

            InventoryTransferItem transferItem = InventoryTransferItem.builder()
                    .transferId(10L)
                    .inventoryId(200L)
                    .quantity(10)
                    .build();
            setField(transferItem, "id", 31L);

            Inventory createdDestInventory = Inventory.builder()
                    .productId(sourceInventory.getProductId())
                    .sellerId(1L)
                    .locationId(2L)
                    .onHandQuantity(0)
                    .build();
            setField(createdDestInventory, "id", 202L);

            ReceiveTransferRequest.ReceivedItemRequest receivedItem = new ReceiveTransferRequest.ReceivedItemRequest();
            receivedItem.setTransferItemId(31L);
            receivedItem.setReceivedQuantity(10);

            ReceiveTransferRequest request = new ReceiveTransferRequest();
            request.setItems(List.of(receivedItem));

            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));
            when(transferItemRepository.findByTransferId(10L)).thenReturn(List.of(transferItem));
            when(inventoryRepository.findById(200L)).thenReturn(Optional.of(sourceInventory));
            when(inventoryRepository.atomicAdjust(eq(200L), eq(-10), any(Instant.class))).thenReturn(1);
            // No destination inventory exists
            when(inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                    sourceInventory.getProductId(), 2L)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(createdDestInventory);
            when(inventoryRepository.atomicAdjust(eq(202L), eq(10), any(Instant.class))).thenReturn(1);
            when(movementRepository.save(any(StockMovement.class))).thenReturn(StockMovement.builder().build());
            when(transferItemRepository.save(transferItem)).thenReturn(transferItem);
            when(transferRepository.save(transfer)).thenReturn(transfer);

            service.receiveTransfer(transferPublicId, request, UUID.randomUUID());

            // New destination inventory should have been created
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when transfer is not APPROVED (still REQUESTED)")
        void receiveTransfer_notApproved_throwsInvalidInventoryOperationException() {
            // transfer.status is REQUESTED by default from setUp
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));

            assertThatThrownBy(() -> service.receiveTransfer(transferPublicId, new ReceiveTransferRequest(), UUID.randomUUID()))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("APPROVED");

            verify(transferItemRepository, never()).findByTransferId(any());
            verify(inventoryRepository, never()).atomicAdjust(any(), anyInt(), any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when transfer is already RECEIVED")
        void receiveTransfer_alreadyReceived_throwsInvalidInventoryOperationException() {
            transfer.setStatus(TransferStatus.RECEIVED);
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));

            assertThatThrownBy(() -> service.receiveTransfer(transferPublicId, new ReceiveTransferRequest(), UUID.randomUUID()))
                    .isInstanceOf(InvalidInventoryOperationException.class);
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when transfer item ID does not match any item in the transfer")
        void receiveTransfer_itemNotFoundInTransfer_throwsInventoryNotFoundException() {
            transfer.setStatus(TransferStatus.APPROVED);

            // The transfer has item with id=99, but the received request references id=999
            InventoryTransferItem transferItem = InventoryTransferItem.builder()
                    .transferId(10L)
                    .inventoryId(200L)
                    .quantity(5)
                    .build();
            setField(transferItem, "id", 99L);

            ReceiveTransferRequest.ReceivedItemRequest receivedItem = new ReceiveTransferRequest.ReceivedItemRequest();
            receivedItem.setTransferItemId(999L); // does not match id=99
            receivedItem.setReceivedQuantity(5);

            ReceiveTransferRequest request = new ReceiveTransferRequest();
            request.setItems(List.of(receivedItem));

            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.of(transfer));
            when(transferItemRepository.findByTransferId(10L)).thenReturn(List.of(transferItem));

            // The stream filter fails before any inventory lookup
            assertThatThrownBy(() -> service.receiveTransfer(transferPublicId, request, UUID.randomUUID()))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining("Transfer item not found");
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when transfer does not exist")
        void receiveTransfer_transferNotFound_throwsInventoryNotFoundException() {
            when(transferRepository.findByPublicId(transferPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.receiveTransfer(transferPublicId, new ReceiveTransferRequest(), UUID.randomUUID()))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining(transferPublicId.toString());
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

package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.AdjustStockRequest;
import ecommerce.modules.inventory.dto.request.CreateInventoryRequest;
import ecommerce.modules.inventory.dto.request.UpdateInventoryRequest;
import ecommerce.modules.inventory.dto.response.AvailabilityResponse;
import ecommerce.modules.inventory.dto.response.InventoryResponse;
import ecommerce.modules.inventory.dto.response.StockMovementResponse;
import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.InventoryStatus;
import ecommerce.modules.inventory.enums.MovementType;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InventoryOwnershipException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryLocationRepository;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.impl.InventoryServiceImpl;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventoryServiceImpl")
class InventoryServiceImplTest {

    @Mock private InventoryRepository         inventoryRepository;
    @Mock private InventoryLocationRepository locationRepository;
    @Mock private StockMovementRepository     movementRepository;
    @Mock private SellerRepository            sellerRepository;
    @Mock private ApplicationEventPublisher   eventPublisher;

    @InjectMocks
    private InventoryServiceImpl service;

    private UUID userId;
    private UUID inventoryPublicId;
    private Seller seller;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        userId            = UUID.randomUUID();
        inventoryPublicId = UUID.randomUUID();

        seller = Seller.builder()
                .ownerUserId(userId)
                .displayName("Test Seller")
                .build();
        setField(seller, "id", 1L);

        inventory = Inventory.builder()
                .productId(UUID.randomUUID())
                .sellerId(1L)
                .locationId(10L)
                .onHandQuantity(50)
                .reservedQuantity(5)
                .lowStockThreshold(5)
                .allowBackorder(false)
                .build();
        setField(inventory, "publicId", inventoryPublicId);
        setField(inventory, "id", 100L);
        setField(inventory, "isActive", true);
    }

    // ─── createInventory ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("createInventory")
    class CreateInventory {

        @Test
        @DisplayName("happy path — no variant, saves inventory, records RECEIPT movement")
        void createInventory_noVariant_savesInventoryAndRecordsMovement() {
            CreateInventoryRequest request = new CreateInventoryRequest();
            request.setProductId(UUID.randomUUID());
            request.setLocationId(10L);
            request.setInitialQuantity(20);
            request.setLowStockThreshold(5);

            ecommerce.modules.inventory.entity.InventoryLocation location =
                    ecommerce.modules.inventory.entity.InventoryLocation.builder()
                            .sellerId(1L).build();
            setField(location, "id", 10L);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
            when(inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                    request.getProductId(), 10L)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
            when(movementRepository.save(any(StockMovement.class))).thenReturn(StockMovement.builder().build());

            InventoryResponse result = service.createInventory(request, userId);

            assertThat(result).isNotNull();
            verify(inventoryRepository).save(any(Inventory.class));
            verify(movementRepository).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("happy path — zero initial quantity, no movement recorded")
        void createInventory_zeroInitialQuantity_noMovementRecorded() {
            CreateInventoryRequest request = new CreateInventoryRequest();
            request.setProductId(UUID.randomUUID());
            request.setLocationId(10L);
            request.setInitialQuantity(0);

            ecommerce.modules.inventory.entity.InventoryLocation location =
                    ecommerce.modules.inventory.entity.InventoryLocation.builder()
                            .sellerId(1L).build();
            setField(location, "id", 10L);

            Inventory zeroInventory = Inventory.builder()
                    .productId(request.getProductId())
                    .sellerId(1L)
                    .locationId(10L)
                    .onHandQuantity(0)
                    .build();
            setField(zeroInventory, "publicId", UUID.randomUUID());
            setField(zeroInventory, "id", 101L);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
            when(inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                    request.getProductId(), 10L)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(zeroInventory);

            service.createInventory(request, userId);

            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when inventory already exists for product+location")
        void createInventory_duplicateRecord_throwsInvalidInventoryOperationException() {
            CreateInventoryRequest request = new CreateInventoryRequest();
            request.setProductId(UUID.randomUUID());
            request.setLocationId(10L);

            ecommerce.modules.inventory.entity.InventoryLocation location =
                    ecommerce.modules.inventory.entity.InventoryLocation.builder()
                            .sellerId(1L).build();
            setField(location, "id", 10L);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
            when(inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                    request.getProductId(), 10L)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> service.createInventory(request, userId))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("already exists");

            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when no seller account exists for user")
        void createInventory_noSellerAccount_throwsInventoryOwnershipException() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createInventory(new CreateInventoryRequest(), userId))
                    .isInstanceOf(InventoryOwnershipException.class)
                    .hasMessageContaining("No seller account found");

            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when location belongs to a different seller")
        void createInventory_locationOwnedByOtherSeller_throwsInventoryOwnershipException() {
            CreateInventoryRequest request = new CreateInventoryRequest();
            request.setProductId(UUID.randomUUID());
            request.setLocationId(10L);

            ecommerce.modules.inventory.entity.InventoryLocation otherLocation =
                    ecommerce.modules.inventory.entity.InventoryLocation.builder()
                            .sellerId(999L).build();
            setField(otherLocation, "id", 10L);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findById(10L)).thenReturn(Optional.of(otherLocation));

            assertThatThrownBy(() -> service.createInventory(request, userId))
                    .isInstanceOf(InventoryOwnershipException.class);

            verify(inventoryRepository, never()).save(any());
        }
    }

    // ─── updateInventory ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateInventory")
    class UpdateInventory {

        @Test
        @DisplayName("happy path — patches lowStockThreshold, allowBackorder, isActive")
        void updateInventory_allFields_patchesSuccessfully() {
            UpdateInventoryRequest request = new UpdateInventoryRequest();
            request.setLowStockThreshold(10);
            request.setAllowBackorder(true);
            request.setIsActive(false);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            InventoryResponse result = service.updateInventory(inventoryPublicId, request, userId);

            assertThat(result).isNotNull();
            assertThat(inventory.getLowStockThreshold()).isEqualTo(10);
            assertThat(inventory.isAllowBackorder()).isTrue();
            assertThat(inventory.getIsActive()).isFalse();
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("null fields are not applied — existing values preserved")
        void updateInventory_nullFields_preservesExistingValues() {
            int originalThreshold = inventory.getLowStockThreshold();
            UpdateInventoryRequest request = new UpdateInventoryRequest();

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(inventory)).thenReturn(inventory);

            service.updateInventory(inventoryPublicId, request, userId);

            assertThat(inventory.getLowStockThreshold()).isEqualTo(originalThreshold);
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when inventory does not exist")
        void updateInventory_notFound_throwsInventoryNotFoundException() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateInventory(inventoryPublicId, new UpdateInventoryRequest(), userId))
                    .isInstanceOf(InventoryNotFoundException.class);
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when inventory belongs to a different seller")
        void updateInventory_wrongOwner_throwsInventoryOwnershipException() {
            Inventory otherSellerInventory = Inventory.builder().sellerId(999L).build();
            setField(otherSellerInventory, "publicId", inventoryPublicId);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(otherSellerInventory));

            assertThatThrownBy(() -> service.updateInventory(inventoryPublicId, new UpdateInventoryRequest(), userId))
                    .isInstanceOf(InventoryOwnershipException.class);

            verify(inventoryRepository, never()).save(any());
        }
    }

    // ─── adjustStock ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("adjustStock")
    class AdjustStock {

        @Test
        @DisplayName("happy path — atomic adjust succeeds, movement recorded, events published")
        void adjustStock_success_recordsMovementAndPublishesEvent() {
            AdjustStockRequest request = new AdjustStockRequest();
            request.setDelta(-10);
            request.setMovementType(MovementType.SALE);
            request.setReason("Order fulfilled");

            StockMovement movement = StockMovement.builder()
                    .inventoryId(100L)
                    .movementType(MovementType.SALE)
                    .quantity(10)
                    .build();

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicAdjust(eq(100L), eq(-10), any(Instant.class))).thenReturn(1);
            when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));
            when(movementRepository.save(any(StockMovement.class))).thenReturn(movement);

            StockMovementResponse result = service.adjustStock(inventoryPublicId, request, userId);

            assertThat(result).isNotNull();
            verify(inventoryRepository).atomicAdjust(eq(100L), eq(-10), any(Instant.class));
            verify(movementRepository).save(any(StockMovement.class));
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when atomic adjust returns 0 (would go negative)")
        void adjustStock_wouldGoNegative_throwsInvalidInventoryOperationException() {
            AdjustStockRequest request = new AdjustStockRequest();
            request.setDelta(-999);
            request.setMovementType(MovementType.ADJUSTMENT);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicAdjust(eq(100L), eq(-999), any(Instant.class))).thenReturn(0);

            assertThatThrownBy(() -> service.adjustStock(inventoryPublicId, request, userId))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("negative stock");

            verify(movementRepository, never()).save(any());
        }

        @Test
        @DisplayName("publishes LowStockDetectedEvent when post-adjust status is LOW_STOCK")
        void adjustStock_lowStockAfterAdjust_publishesLowStockEvent() {
            // Set inventory so available = 3, threshold = 5 => LOW_STOCK
            setField(inventory, "onHandQuantity", 8);
            setField(inventory, "reservedQuantity", 5);
            setField(inventory, "lowStockThreshold", 5);

            AdjustStockRequest request = new AdjustStockRequest();
            request.setDelta(-5);
            request.setMovementType(MovementType.ADJUSTMENT);

            // After reload, onHandQuantity = 3 (available = 3-0 = 3, <= threshold 5)
            Inventory reloadedInventory = Inventory.builder()
                    .onHandQuantity(3)
                    .reservedQuantity(0)
                    .lowStockThreshold(5)
                    .sellerId(1L)
                    .build();
            setField(reloadedInventory, "publicId", inventoryPublicId);
            setField(reloadedInventory, "id", 100L);
            setField(reloadedInventory, "isActive", true);

            StockMovement movement = StockMovement.builder().inventoryId(100L).build();

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.atomicAdjust(eq(100L), eq(-5), any(Instant.class))).thenReturn(1);
            when(inventoryRepository.findById(100L)).thenReturn(Optional.of(reloadedInventory));
            when(movementRepository.save(any(StockMovement.class))).thenReturn(movement);

            service.adjustStock(inventoryPublicId, request, userId);

            verify(eventPublisher).publishEvent(any(ecommerce.modules.inventory.event.LowStockDetectedEvent.class));
        }
    }

    // ─── getInventory ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getInventory")
    class GetInventory {

        @Test
        @DisplayName("happy path — returns InventoryResponse for the seller's inventory")
        void getInventory_happyPath_returnsResponse() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(inventory));

            InventoryResponse result = service.getInventory(inventoryPublicId, userId);

            assertThat(result).isNotNull();
            assertThat(result.getPublicId()).isEqualTo(inventoryPublicId);
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when inventory does not exist")
        void getInventory_notFound_throwsInventoryNotFoundException() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getInventory(inventoryPublicId, userId))
                    .isInstanceOf(InventoryNotFoundException.class);
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when inventory belongs to a different seller")
        void getInventory_wrongOwner_throwsInventoryOwnershipException() {
            Inventory otherInventory = Inventory.builder().sellerId(999L).build();
            setField(otherInventory, "publicId", inventoryPublicId);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findByPublicId(inventoryPublicId)).thenReturn(Optional.of(otherInventory));

            assertThatThrownBy(() -> service.getInventory(inventoryPublicId, userId))
                    .isInstanceOf(InventoryOwnershipException.class);
        }
    }

    // ─── getSellerInventory ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getSellerInventory")
    class GetSellerInventory {

        @Test
        @DisplayName("happy path — returns paged inventory belonging to the seller")
        void getSellerInventory_happyPath_returnsPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Inventory> inventoryPage = new PageImpl<>(List.of(inventory));

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findBySellerIdAndIsActiveTrue(1L, pageable)).thenReturn(inventoryPage);

            Page<InventoryResponse> result = service.getSellerInventory(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns empty page when seller has no inventory")
        void getSellerInventory_noInventory_returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Inventory> emptyPage = Page.empty();

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(inventoryRepository.findBySellerIdAndIsActiveTrue(1L, pageable)).thenReturn(emptyPage);

            Page<InventoryResponse> result = service.getSellerInventory(userId, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─── getAvailability ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailability")
    class GetAvailability {

        @Test
        @DisplayName("returns IN_STOCK when total available quantity exceeds threshold")
        void getAvailability_sufficientStock_returnsInStock() {
            UUID productId = UUID.randomUUID();
            Inventory inStockInventory = Inventory.builder()
                    .productId(productId)
                    .onHandQuantity(50)
                    .reservedQuantity(0)
                    .lowStockThreshold(5)
                    .allowBackorder(false)
                    .build();
            setField(inStockInventory, "isActive", true);

            when(inventoryRepository.findByProductIdAndIsActiveTrue(productId))
                    .thenReturn(List.of(inStockInventory));

            AvailabilityResponse result = service.getAvailability(productId, null);

            assertThat(result.getStatus()).isEqualTo(InventoryStatus.IN_STOCK);
            assertThat(result.getAvailableQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("returns LOW_STOCK when available quantity is positive but at or below threshold of 5")
        void getAvailability_lowStock_returnsLowStock() {
            UUID productId = UUID.randomUUID();
            Inventory lowInventory = Inventory.builder()
                    .productId(productId)
                    .onHandQuantity(3)
                    .reservedQuantity(0)
                    .lowStockThreshold(5)
                    .allowBackorder(false)
                    .build();
            setField(lowInventory, "isActive", true);

            when(inventoryRepository.findByProductIdAndIsActiveTrue(productId))
                    .thenReturn(List.of(lowInventory));

            AvailabilityResponse result = service.getAvailability(productId, null);

            assertThat(result.getStatus()).isEqualTo(InventoryStatus.LOW_STOCK);
            assertThat(result.getAvailableQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("returns OUT_OF_STOCK when no available quantity and backorder not allowed")
        void getAvailability_outOfStock_returnsOutOfStock() {
            UUID productId = UUID.randomUUID();
            Inventory emptyInventory = Inventory.builder()
                    .productId(productId)
                    .onHandQuantity(0)
                    .reservedQuantity(0)
                    .allowBackorder(false)
                    .build();
            setField(emptyInventory, "isActive", true);

            when(inventoryRepository.findByProductIdAndIsActiveTrue(productId))
                    .thenReturn(List.of(emptyInventory));

            AvailabilityResponse result = service.getAvailability(productId, null);

            assertThat(result.getStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);
            assertThat(result.isAllowBackorder()).isFalse();
        }

        @Test
        @DisplayName("returns IN_STOCK when out-of-stock but backorder is allowed")
        void getAvailability_outOfStockWithBackorder_returnsInStock() {
            UUID productId = UUID.randomUUID();
            Inventory backorderInventory = Inventory.builder()
                    .productId(productId)
                    .onHandQuantity(0)
                    .reservedQuantity(0)
                    .allowBackorder(true)
                    .build();
            setField(backorderInventory, "isActive", true);

            when(inventoryRepository.findByProductIdAndIsActiveTrue(productId))
                    .thenReturn(List.of(backorderInventory));

            AvailabilityResponse result = service.getAvailability(productId, null);

            assertThat(result.isAllowBackorder()).isTrue();
            assertThat(result.getStatus()).isEqualTo(InventoryStatus.IN_STOCK);
        }

        @Test
        @DisplayName("filters by variantId when provided — ignores records for other variants")
        void getAvailability_withVariantId_filtersToMatchingVariant() {
            UUID productId = UUID.randomUUID();
            UUID variantId = UUID.randomUUID();

            Inventory variantInventory = Inventory.builder()
                    .productId(productId)
                    .variantId(variantId)
                    .onHandQuantity(20)
                    .reservedQuantity(0)
                    .lowStockThreshold(5)
                    .allowBackorder(false)
                    .build();
            setField(variantInventory, "isActive", true);

            Inventory otherVariantInventory = Inventory.builder()
                    .productId(productId)
                    .variantId(UUID.randomUUID())
                    .onHandQuantity(100)
                    .reservedQuantity(0)
                    .build();
            setField(otherVariantInventory, "isActive", true);

            when(inventoryRepository.findByProductIdAndIsActiveTrue(productId))
                    .thenReturn(List.of(variantInventory, otherVariantInventory));

            AvailabilityResponse result = service.getAvailability(productId, variantId);

            assertThat(result.getAvailableQuantity()).isEqualTo(20);
            assertThat(result.getVariantId()).isEqualTo(variantId);
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

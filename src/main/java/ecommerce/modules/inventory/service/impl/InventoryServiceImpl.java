package ecommerce.modules.inventory.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.inventory.dto.request.AdjustStockRequest;
import ecommerce.modules.inventory.dto.request.CreateInventoryRequest;
import ecommerce.modules.inventory.dto.request.UpdateInventoryRequest;
import ecommerce.modules.inventory.dto.response.AvailabilityResponse;
import ecommerce.modules.inventory.dto.response.InventoryResponse;
import ecommerce.modules.inventory.dto.response.StockMovementResponse;
import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.InventoryStatus;
import ecommerce.modules.inventory.event.LowStockDetectedEvent;
import ecommerce.modules.inventory.event.OutOfStockEvent;
import ecommerce.modules.inventory.event.StockAdjustedEvent;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InventoryOwnershipException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryLocationRepository;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.InventorySummaryViewRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.InventoryService;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository             inventoryRepository;
    private final InventoryLocationRepository     locationRepository;
    private final InventorySummaryViewRepository  inventorySummaryViewRepository;
    private final StockMovementRepository         movementRepository;
    private final SellerRepository                sellerRepository;
    private final ApplicationEventPublisher       eventPublisher;

    @Override
    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request, UUID userId) {
        Seller seller = resolveSeller(userId);

        verifyLocationOwnership(request.getLocationId(), seller.getId());

        boolean exists = request.getVariantId() == null
                ? inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                        request.getProductId(), request.getLocationId()).isPresent()
                : inventoryRepository.findByProductIdAndVariantIdAndLocationId(
                        request.getProductId(), request.getVariantId(), request.getLocationId()).isPresent();

        if (exists) {
            throw new InvalidInventoryOperationException(
                    "Inventory record already exists for this product/variant at the given location");
        }

        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .variantId(request.getVariantId())
                .sellerId(seller.getId())
                .storeId(request.getStoreId())
                .locationId(request.getLocationId())
                .onHandQuantity(request.getInitialQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .allowBackorder(request.isAllowBackorder())
                .build();

        inventory = inventoryRepository.save(inventory);

        if (request.getInitialQuantity() > 0) {
            recordMovement(inventory, request.getInitialQuantity(),
                    ecommerce.modules.inventory.enums.MovementType.RECEIPT,
                    0, request.getInitialQuantity(), "Initial stock", userId);
        }

        log.info("Inventory created for product={} at location={}", request.getProductId(), request.getLocationId());
        return InventoryResponse.from(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(UUID publicId, UpdateInventoryRequest request, UUID userId) {
        Seller seller = resolveSeller(userId);
        Inventory inventory = findAndVerifyOwnership(publicId, seller.getId());

        if (request.getLowStockThreshold() != null) inventory.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getAllowBackorder() != null) inventory.setAllowBackorder(request.getAllowBackorder());
        if (request.getIsActive() != null) inventory.setIsActive(request.getIsActive());

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Override
    public InventoryResponse getInventory(UUID publicId, UUID userId) {
        Seller seller = resolveSeller(userId);
        return InventoryResponse.from(findAndVerifyOwnership(publicId, seller.getId()));
    }

    @Override
    public Page<InventoryResponse> getSellerInventory(UUID userId, Pageable pageable) {
        Seller seller = resolveSeller(userId);
        return inventorySummaryViewRepository.findBySellerIdAndIsActiveTrue(seller.getId(), pageable)
                .map(InventoryResponse::from);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTORY_AVAILABILITY, allEntries = true)
    public StockMovementResponse adjustStock(UUID publicId, AdjustStockRequest request, UUID userId) {
        Seller seller = resolveSeller(userId);
        Inventory inventory = findAndVerifyOwnership(publicId, seller.getId());

        int previous = inventory.getOnHandQuantity();
        int rowsAffected = inventoryRepository.atomicAdjust(inventory.getId(), request.getDelta(), Instant.now());
        if (rowsAffected == 0) {
            throw new InvalidInventoryOperationException(
                    "Adjustment would result in negative stock — rejected");
        }

        // Re-load to get updated quantity after atomic update
        inventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        int newQty = inventory.getOnHandQuantity();

        StockMovement movement = recordMovement(inventory, Math.abs(request.getDelta()),
                request.getMovementType(), previous, newQty, request.getReason(), userId);

        eventPublisher.publishEvent(new StockAdjustedEvent(inventory.getPublicId(), request.getDelta(), request.getMovementType()));

        InventoryStatus status = inventory.getStatus();
        if (status == InventoryStatus.LOW_STOCK) {
            eventPublisher.publishEvent(new LowStockDetectedEvent(
                    inventory.getPublicId(), inventory.getAvailableQuantity(), inventory.getLowStockThreshold()));
        } else if (status == InventoryStatus.OUT_OF_STOCK) {
            eventPublisher.publishEvent(new OutOfStockEvent(inventory.getPublicId(), inventory.getProductId()));
        }

        log.info("Stock adjusted for inventory={} delta={} type={}", publicId, request.getDelta(), request.getMovementType());
        return StockMovementResponse.from(movement);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.INVENTORY_AVAILABILITY, key = "#productId + ':' + #variantId")
    public AvailabilityResponse getAvailability(UUID productId, UUID variantId) {
        List<Inventory> records = inventoryRepository.findByProductIdAndIsActiveTrue(productId);

        int totalAvailable = records.stream()
                .filter(i -> variantId == null ? i.getVariantId() == null : variantId.equals(i.getVariantId()))
                .mapToInt(Inventory::getAvailableQuantity)
                .sum();

        boolean allowBackorder = records.stream()
                .filter(i -> variantId == null ? i.getVariantId() == null : variantId.equals(i.getVariantId()))
                .anyMatch(Inventory::isAllowBackorder);

        InventoryStatus status;
        if (totalAvailable == 0 && !allowBackorder) {
            status = InventoryStatus.OUT_OF_STOCK;
        } else if (totalAvailable <= 5 && totalAvailable > 0) {
            status = InventoryStatus.LOW_STOCK;
        } else {
            status = InventoryStatus.IN_STOCK;
        }

        return AvailabilityResponse.builder()
                .productId(productId)
                .variantId(variantId)
                .availableQuantity(totalAvailable)
                .status(status)
                .allowBackorder(allowBackorder)
                .build();
    }

    private StockMovement recordMovement(Inventory inventory, int quantity,
                                         ecommerce.modules.inventory.enums.MovementType type,
                                         int previous, int newQty, String reason, UUID performedBy) {
        StockMovement movement = StockMovement.builder()
                .inventoryId(inventory.getId())
                .movementType(type)
                .quantity(quantity)
                .previousQuantity(previous)
                .newQuantity(newQty)
                .reason(reason)
                .performedBy(performedBy)
                .build();
        return movementRepository.save(movement);
    }

    private Seller resolveSeller(UUID userId) {
        return sellerRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new InventoryOwnershipException("No seller account found for user " + userId));
    }

    private Inventory findAndVerifyOwnership(UUID publicId, Long sellerId) {
        Inventory inventory = inventoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found: " + publicId));
        if (!inventory.getSellerId().equals(sellerId)) {
            throw new InventoryOwnershipException("Inventory does not belong to this seller");
        }
        return inventory;
    }

    private void verifyLocationOwnership(Long locationId, Long sellerId) {
        locationRepository.findById(locationId)
                .filter(loc -> loc.getSellerId().equals(sellerId))
                .orElseThrow(() -> new InventoryOwnershipException(
                        "Location " + locationId + " does not belong to this seller"));
    }
}

package ecommerce.modules.inventory.service.impl;

import ecommerce.modules.inventory.dto.request.CreateTransferRequest;
import ecommerce.modules.inventory.dto.request.ReceiveTransferRequest;
import ecommerce.modules.inventory.dto.response.InventoryTransferResponse;
import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.entity.InventoryTransfer;
import ecommerce.modules.inventory.entity.InventoryTransferItem;
import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.MovementType;
import ecommerce.modules.inventory.enums.TransferStatus;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.InventoryTransferItemRepository;
import ecommerce.modules.inventory.repository.InventoryTransferRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.InventoryTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class InventoryTransferServiceImpl implements InventoryTransferService {

    private final InventoryTransferRepository     transferRepository;
    private final InventoryTransferItemRepository transferItemRepository;
    private final InventoryRepository             inventoryRepository;
    private final StockMovementRepository         movementRepository;

    @Override
    @Transactional
    public InventoryTransferResponse requestTransfer(CreateTransferRequest request, UUID requestedBy) {
        if (request.getSourceLocationId().equals(request.getDestinationLocationId())) {
            throw new InvalidInventoryOperationException("Source and destination locations must be different");
        }

        InventoryTransfer transfer = InventoryTransfer.builder()
                .sourceLocationId(request.getSourceLocationId())
                .destinationLocationId(request.getDestinationLocationId())
                .requestedBy(requestedBy)
                .build();
        transfer = transferRepository.save(transfer);

        for (CreateTransferRequest.TransferItemRequest item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByPublicId(item.getInventoryPublicId())
                    .orElseThrow(() -> new InventoryNotFoundException(
                            "Inventory not found: " + item.getInventoryPublicId()));

            InventoryTransferItem transferItem = InventoryTransferItem.builder()
                    .transferId(transfer.getId())
                    .inventoryId(inventory.getId())
                    .quantity(item.getQuantity())
                    .build();
            transferItemRepository.save(transferItem);
        }

        log.info("Transfer requested from location={} to location={}", request.getSourceLocationId(), request.getDestinationLocationId());
        return InventoryTransferResponse.from(transfer);
    }

    @Override
    @Transactional
    public InventoryTransferResponse approveTransfer(UUID transferPublicId, UUID approvedBy) {
        InventoryTransfer transfer = findByPublicId(transferPublicId);
        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            throw new InvalidInventoryOperationException("Transfer must be REQUESTED to approve. Current: " + transfer.getStatus());
        }
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(approvedBy);
        log.info("Transfer approved: {}", transferPublicId);
        return InventoryTransferResponse.from(transferRepository.save(transfer));
    }

    @Override
    @Transactional
    public InventoryTransferResponse receiveTransfer(UUID transferPublicId, ReceiveTransferRequest request, UUID receivedBy) {
        InventoryTransfer transfer = findByPublicId(transferPublicId);
        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw new InvalidInventoryOperationException("Transfer must be APPROVED to receive. Current: " + transfer.getStatus());
        }

        List<InventoryTransferItem> items = transferItemRepository.findByTransferId(transfer.getId());

        for (ReceiveTransferRequest.ReceivedItemRequest received : request.getItems()) {
            InventoryTransferItem item = items.stream()
                    .filter(i -> i.getId().equals(received.getTransferItemId()))
                    .findFirst()
                    .orElseThrow(() -> new InventoryNotFoundException("Transfer item not found: " + received.getTransferItemId()));

            Inventory sourceInventory = inventoryRepository.findById(item.getInventoryId()).orElseThrow();
            int qty = received.getReceivedQuantity();

            // Deduct from source (TRANSFER_OUT)
            inventoryRepository.atomicAdjust(sourceInventory.getId(), -qty, Instant.now());
            movementRepository.save(StockMovement.builder()
                    .inventoryId(sourceInventory.getId())
                    .movementType(MovementType.TRANSFER_OUT)
                    .quantity(qty)
                    .previousQuantity(sourceInventory.getOnHandQuantity())
                    .newQuantity(Math.toIntExact((long) sourceInventory.getOnHandQuantity() - qty))
                    .referenceType("TRANSFER")
                    .referenceId(transfer.getPublicId())
                    .performedBy(receivedBy)
                    .build());

            // Add to destination inventory (find or create)
            Long destLocationId = transfer.getDestinationLocationId();
            Inventory destInventory = (sourceInventory.getVariantId() == null)
                    ? inventoryRepository.findByProductIdAndLocationIdAndVariantIdIsNull(
                            sourceInventory.getProductId(), destLocationId)
                            .orElseGet(() -> createDestinationInventory(sourceInventory, destLocationId))
                    : inventoryRepository.findByProductIdAndVariantIdAndLocationId(
                            sourceInventory.getProductId(), sourceInventory.getVariantId(), destLocationId)
                            .orElseGet(() -> createDestinationInventory(sourceInventory, destLocationId));

            inventoryRepository.atomicAdjust(destInventory.getId(), qty, Instant.now());
            movementRepository.save(StockMovement.builder()
                    .inventoryId(destInventory.getId())
                    .movementType(MovementType.TRANSFER_IN)
                    .quantity(qty)
                    .previousQuantity(destInventory.getOnHandQuantity())
                    .newQuantity(Math.toIntExact((long) destInventory.getOnHandQuantity() + qty))
                    .referenceType("TRANSFER")
                    .referenceId(transfer.getPublicId())
                    .performedBy(receivedBy)
                    .build());

            item.setReceivedQuantity(received.getReceivedQuantity());
            transferItemRepository.save(item);
        }

        transfer.setStatus(TransferStatus.RECEIVED);
        transfer.setCompletedAt(Instant.now());
        log.info("Transfer received: {}", transferPublicId);
        return InventoryTransferResponse.from(transferRepository.save(transfer));
    }

    @Override
    @Transactional
    public InventoryTransferResponse cancelTransfer(UUID transferPublicId) {
        InventoryTransfer transfer = findByPublicId(transferPublicId);
        if (transfer.getStatus() == TransferStatus.RECEIVED || transfer.getStatus() == TransferStatus.CANCELLED) {
            throw new InvalidInventoryOperationException("Cannot cancel transfer with status: " + transfer.getStatus());
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        log.info("Transfer cancelled: {}", transferPublicId);
        return InventoryTransferResponse.from(transferRepository.save(transfer));
    }

    @Override
    public Page<InventoryTransferResponse> getTransfers(Pageable pageable) {
        return transferRepository.findAll(pageable).map(InventoryTransferResponse::from);
    }

    private InventoryTransfer findByPublicId(UUID publicId) {
        return transferRepository.findByPublicId(publicId)
                .orElseThrow(() -> new InventoryNotFoundException("Transfer not found: " + publicId));
    }

    private Inventory createDestinationInventory(Inventory source, Long destLocationId) {
        Inventory dest = Inventory.builder()
                .productId(source.getProductId())
                .variantId(source.getVariantId())
                .sellerId(source.getSellerId())
                .storeId(source.getStoreId())
                .locationId(destLocationId)
                .lowStockThreshold(source.getLowStockThreshold())
                .allowBackorder(source.isAllowBackorder())
                .build();
        return inventoryRepository.save(dest);
    }
}

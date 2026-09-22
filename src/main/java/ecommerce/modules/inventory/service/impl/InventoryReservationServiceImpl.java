package ecommerce.modules.inventory.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.inventory.dto.request.ReserveStockRequest;
import ecommerce.modules.inventory.dto.response.InventoryReservationResponse;
import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.entity.InventoryReservation;
import ecommerce.modules.inventory.entity.StockMovement;
import ecommerce.modules.inventory.enums.InventoryReservationStatus;
import ecommerce.modules.inventory.enums.MovementType;
import ecommerce.modules.inventory.event.StockReleasedEvent;
import ecommerce.modules.inventory.event.StockReservedEvent;
import ecommerce.modules.inventory.exception.InsufficientStockException;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryRepository;
import ecommerce.modules.inventory.repository.InventoryReservationRepository;
import ecommerce.modules.inventory.repository.StockMovementRepository;
import ecommerce.modules.inventory.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryRepository            inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final StockMovementRepository        movementRepository;
    private final ApplicationEventPublisher      eventPublisher;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTORY_AVAILABILITY, allEntries = true)
    public InventoryReservationResponse reserve(UUID inventoryPublicId, ReserveStockRequest request) {
        Inventory inventory = inventoryRepository.findByPublicId(inventoryPublicId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found: " + inventoryPublicId));

        int qty = request.getQuantity();
        int rowsAffected = inventoryRepository.atomicReserve(inventory.getId(), qty, Instant.now());
        if (rowsAffected == 0) {
            throw new InsufficientStockException(
                    "Insufficient stock for inventory " + inventoryPublicId + ". Requested: " + qty);
        }

        InventoryReservation reservation = InventoryReservation.builder()
                .inventoryId(inventory.getId())
                .orderId(request.getOrderId())
                .quantity(qty)
                .expiresAt(request.getExpiresAt())
                .build();
        reservation = reservationRepository.save(reservation);

        movementRepository.save(StockMovement.builder()
                .inventoryId(inventory.getId())
                .movementType(MovementType.RESERVATION)
                .quantity(qty)
                .previousQuantity(inventory.getReservedQuantity())
                .newQuantity(Math.toIntExact((long) inventory.getReservedQuantity() + qty))
                .referenceType("RESERVATION")
                .referenceId(reservation.getPublicId())
                .build());

        eventPublisher.publishEvent(new StockReservedEvent(inventoryPublicId, qty, request.getOrderId()));
        log.info("Reserved {} units of inventory={}", qty, inventoryPublicId);
        return InventoryReservationResponse.from(reservation);
    }

    @Override
    @Transactional
    public InventoryReservationResponse confirm(UUID reservationPublicId) {
        InventoryReservation reservation = findActiveReservation(reservationPublicId);
        Inventory inventory = inventoryRepository.findById(reservation.getInventoryId()).orElseThrow();

        int qty = reservation.getQuantity();
        int rowsAffected = inventoryRepository.atomicCommit(inventory.getId(), qty, Instant.now());
        if (rowsAffected == 0) {
            throw new InvalidInventoryOperationException("Cannot confirm reservation — insufficient committed stock");
        }

        reservation.setStatus(InventoryReservationStatus.CONFIRMED);
        movementRepository.save(StockMovement.builder()
                .inventoryId(inventory.getId())
                .movementType(MovementType.SALE)
                .quantity(qty)
                .previousQuantity(inventory.getOnHandQuantity())
                .newQuantity(inventory.getOnHandQuantity() - qty)
                .referenceType("RESERVATION")
                .referenceId(reservation.getPublicId())
                .build());

        log.info("Confirmed reservation={}", reservationPublicId);
        return InventoryReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTORY_AVAILABILITY, allEntries = true)
    public InventoryReservationResponse release(UUID reservationPublicId) {
        InventoryReservation reservation = reservationRepository.findByPublicId(reservationPublicId)
                .orElseThrow(() -> new InventoryNotFoundException("Reservation not found: " + reservationPublicId));

        if (reservation.getStatus() == InventoryReservationStatus.RELEASED
                || reservation.getStatus() == InventoryReservationStatus.EXPIRED) {
            throw new InvalidInventoryOperationException("Reservation is already " + reservation.getStatus());
        }

        Inventory inventory = inventoryRepository.findById(reservation.getInventoryId()).orElseThrow();
        int qty = reservation.getQuantity();

        inventoryRepository.atomicRelease(inventory.getId(), qty, Instant.now());

        reservation.setStatus(InventoryReservationStatus.RELEASED);
        reservation.setReleasedAt(Instant.now());

        movementRepository.save(StockMovement.builder()
                .inventoryId(inventory.getId())
                .movementType(MovementType.RELEASE)
                .quantity(qty)
                .previousQuantity(inventory.getReservedQuantity())
                .newQuantity(inventory.getReservedQuantity() - qty)
                .referenceType("RESERVATION")
                .referenceId(reservation.getPublicId())
                .build());

        eventPublisher.publishEvent(new StockReleasedEvent(inventory.getPublicId(), qty, reservation.getOrderId()));
        log.info("Released reservation={}", reservationPublicId);
        return InventoryReservationResponse.from(reservationRepository.save(reservation));
    }

    @Override
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireStale() {
        List<InventoryReservation> expired = reservationRepository.findExpiredActive(Instant.now());
        if (expired.isEmpty()) return;

        log.info("Expiring {} stale reservations", expired.size());
        for (InventoryReservation r : expired) {
            try {
                inventoryRepository.atomicRelease(r.getInventoryId(), r.getQuantity(), Instant.now());
                r.setStatus(InventoryReservationStatus.EXPIRED);
                r.setReleasedAt(Instant.now());
                reservationRepository.save(r);
            } catch (Exception e) {
                log.error("Failed to expire reservation={}", r.getPublicId(), e);
            }
        }
    }

    private InventoryReservation findActiveReservation(UUID publicId) {
        InventoryReservation r = reservationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new InventoryNotFoundException("Reservation not found: " + publicId));
        if (r.getStatus() != InventoryReservationStatus.ACTIVE) {
            throw new InvalidInventoryOperationException("Reservation is not ACTIVE: " + r.getStatus());
        }
        return r;
    }
}

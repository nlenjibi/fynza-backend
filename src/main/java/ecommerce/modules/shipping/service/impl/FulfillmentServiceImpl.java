package ecommerce.modules.shipping.service.impl;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.shipping.dto.request.CreateFulfillmentRequest;
import ecommerce.modules.shipping.dto.response.FulfillmentResponse;
import ecommerce.modules.shipping.dto.response.ShipmentItemResponse;
import ecommerce.modules.shipping.dto.response.ShipmentResponse;
import ecommerce.modules.shipping.dto.response.TrackingEventResponse;
import ecommerce.modules.shipping.entity.Fulfillment;
import ecommerce.modules.shipping.enums.FulfillmentStatus;
import ecommerce.modules.shipping.exception.FulfillmentNotFoundException;
import ecommerce.modules.shipping.repository.FulfillmentRepository;
import ecommerce.modules.shipping.repository.ShipmentItemRepository;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.repository.TrackingEventRepository;
import ecommerce.modules.shipping.service.FulfillmentService;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FulfillmentServiceImpl implements FulfillmentService {

    private static final List<FulfillmentStatus> TERMINAL_STATUSES =
            List.of(FulfillmentStatus.COMPLETED, FulfillmentStatus.CANCELLED);

    private final FulfillmentRepository fulfillmentRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional
    public FulfillmentResponse createFulfillment(UUID userId, CreateFulfillmentRequest request) {
        Long sellerId = resolveSellerLongId(userId);

        if (fulfillmentRepository.existsBySellerOrderId(request.getSellerOrderId())) {
            Fulfillment existing = fulfillmentRepository.findBySellerOrderId(request.getSellerOrderId()).orElseThrow();
            if (!existing.getSellerId().equals(sellerId)) {
                throw new ForbiddenException("You do not own this fulfillment");
            }
            return FulfillmentResponse.from(existing, List.of());
        }

        Fulfillment fulfillment = Fulfillment.builder()
                .orderId(request.getOrderId())
                .sellerOrderId(request.getSellerOrderId())
                .sellerId(sellerId)
                .notes(request.getNotes())
                .build();
        fulfillment = fulfillmentRepository.save(fulfillment);
        log.info("Created fulfillment id={} for seller={}", fulfillment.getPublicId(), sellerId);
        return FulfillmentResponse.from(fulfillment, List.of());
    }

    @Override
    public FulfillmentResponse getFulfillment(UUID fulfillmentPublicId) {
        Fulfillment fulfillment = findOrThrow(fulfillmentPublicId);
        return toResponse(fulfillment);
    }

    @Override
    public FulfillmentResponse getFulfillmentForSeller(UUID userId, UUID fulfillmentPublicId) {
        Long sellerId = resolveSellerLongId(userId);
        Fulfillment fulfillment = findOrThrow(fulfillmentPublicId);
        if (!fulfillment.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You do not own this fulfillment");
        }
        return toResponse(fulfillment);
    }

    @Override
    public Page<FulfillmentResponse> getSellerFulfillments(UUID userId, FulfillmentStatus status, Pageable pageable) {
        Long sellerId = resolveSellerLongId(userId);
        Page<Fulfillment> page = status != null
                ? fulfillmentRepository.findBySellerIdAndStatus(sellerId, status, pageable)
                : fulfillmentRepository.findBySellerId(sellerId, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional
    public FulfillmentResponse updateStatus(UUID userId, UUID fulfillmentPublicId,
                                            FulfillmentStatus newStatus, String notes) {
        Long sellerId = resolveSellerLongId(userId);
        Fulfillment fulfillment = findOrThrow(fulfillmentPublicId);

        if (!fulfillment.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You do not own this fulfillment");
        }
        if (TERMINAL_STATUSES.contains(fulfillment.getStatus())) {
            throw new IllegalStateException("Fulfillment is already in terminal status: " + fulfillment.getStatus());
        }

        FulfillmentStatus prev = fulfillment.getStatus();
        fulfillment.setStatus(newStatus);
        if (notes != null) fulfillment.setNotes(notes);

        Instant now = Instant.now();
        switch (newStatus) {
            case PACKED -> fulfillment.setPackedAt(now);
            case SHIPPED -> fulfillment.setShippedAt(now);
            case COMPLETED -> fulfillment.setCompletedAt(now);
            case CANCELLED -> fulfillment.setCancelledAt(now);
            default -> { /* no timestamp for PROCESSING, READY_TO_SHIP */ }
        }

        fulfillmentRepository.save(fulfillment);
        log.info("Fulfillment id={} status {} -> {}", fulfillmentPublicId, prev, newStatus);
        return toResponse(fulfillment);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Fulfillment findOrThrow(UUID publicId) {
        return fulfillmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new FulfillmentNotFoundException("Fulfillment not found: " + publicId));
    }

    private FulfillmentResponse toResponse(Fulfillment fulfillment) {
        List<ShipmentResponse> shipments = shipmentRepository
                .findByFulfillment_PublicId(fulfillment.getPublicId())
                .stream()
                .map(shipment -> {
                    List<ShipmentItemResponse> items = shipmentItemRepository
                            .findByShipment_PublicId(shipment.getPublicId())
                            .stream().map(ShipmentItemResponse::from).toList();
                    List<TrackingEventResponse> events = trackingEventRepository
                            .findByShipment_PublicIdOrderByOccurredAtDesc(shipment.getPublicId())
                            .stream().map(TrackingEventResponse::from).toList();
                    return ShipmentResponse.from(shipment, items, events);
                })
                .toList();
        return FulfillmentResponse.from(fulfillment, shipments);
    }

    private Long resolveSellerLongId(UUID userId) {
        return sellerRepository.findByOwnerUserId(userId)
                .map(s -> s.getId())
                .orElseThrow(() -> new ForbiddenException("User is not a registered seller"));
    }
}

package ecommerce.modules.shipping.service.impl;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.shipping.dto.request.CreateShipmentRequest;
import ecommerce.modules.shipping.dto.request.RecordTrackingEventRequest;
import ecommerce.modules.shipping.dto.request.UpdateShipmentStatusRequest;
import ecommerce.modules.shipping.dto.response.ShipmentItemResponse;
import ecommerce.modules.shipping.dto.response.ShipmentResponse;
import ecommerce.modules.shipping.dto.response.TrackingEventResponse;
import ecommerce.modules.shipping.entity.*;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.event.ShipmentCreatedEvent;
import ecommerce.modules.shipping.event.ShipmentDeliveredEvent;
import ecommerce.modules.shipping.event.ShipmentStatusChangedEvent;
import ecommerce.modules.shipping.exception.FulfillmentNotFoundException;
import ecommerce.modules.shipping.exception.InvalidShipmentTransitionException;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.provider.ShipmentProviderRequest;
import ecommerce.modules.shipping.provider.ShipmentProviderResult;
import ecommerce.modules.shipping.provider.ShippingProvider;
import ecommerce.modules.shipping.repository.*;
import ecommerce.modules.shipping.service.ShipmentService;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShipmentServiceImpl implements ShipmentService {

    private static final Set<ShipmentStatus> CANCELLABLE_STATUSES = Set.of(
            ShipmentStatus.DRAFT,
            ShipmentStatus.READY,
            ShipmentStatus.LABEL_CREATED,
            ShipmentStatus.PICKUP_SCHEDULED
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final CarrierRepository carrierRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingLabelRepository shippingLabelRepository;
    private final SellerRepository sellerRepository;
    private final ShippingProvider shippingProvider;
    private final FynzaEventPublisher eventPublisher;

    @Override
    @Transactional
    public ShipmentResponse createShipment(UUID userId, CreateShipmentRequest request) {
        Long sellerId = resolveSellerLongId(userId);

        Fulfillment fulfillment = fulfillmentRepository.findByPublicId(request.getFulfillmentId())
                .orElseThrow(() -> new FulfillmentNotFoundException("Fulfillment not found: " + request.getFulfillmentId()));

        if (!fulfillment.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You do not own this fulfillment");
        }

        Carrier carrier = request.getCarrierId() != null
                ? carrierRepository.findByPublicId(request.getCarrierId()).orElse(null)
                : null;
        ShippingMethod method = request.getShippingMethodId() != null
                ? shippingMethodRepository.findByPublicId(request.getShippingMethodId()).orElse(null)
                : null;

        ShipmentAddress address = buildAddress(request);

        Shipment shipment = Shipment.builder()
                .shipmentNumber(generateShipmentNumber())
                .fulfillment(fulfillment)
                .carrier(carrier)
                .shippingMethod(method)
                .address(address)
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .shippingCost(request.getShippingCost() != null ? request.getShippingCost() : java.math.BigDecimal.ZERO)
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .build();
        shipment = shipmentRepository.save(shipment);

        List<ShipmentItem> items = saveItems(shipment, request);
        recordEvent(shipment, ShipmentStatus.DRAFT, "Shipment created", null);

        eventPublisher.publish(new ShipmentCreatedEvent(
                shipment.getPublicId(),
                shipment.getShipmentNumber(),
                fulfillment.getPublicId(),
                fulfillment.getOrderId(),
                sellerId));

        log.info("Created shipment number={} for fulfillment={}", shipment.getShipmentNumber(), fulfillment.getPublicId());
        return toResponse(shipment, items, List.of());
    }

    @Override
    public ShipmentResponse getShipment(UUID shipmentPublicId) {
        return toResponse(findOrThrow(shipmentPublicId));
    }

    @Override
    public ShipmentResponse getByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found for tracking: " + trackingNumber));
        return toResponse(shipment);
    }

    @Override
    public List<ShipmentResponse> getShipmentsForOrder(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ShipmentResponse updateStatus(UUID userId, UUID shipmentPublicId, UpdateShipmentStatusRequest request) {
        Long sellerId = resolveSellerLongId(userId);
        Shipment shipment = findOrThrow(shipmentPublicId);
        assertSellerOwns(shipment, sellerId);

        ShipmentStatus prev = shipment.getStatus();
        ShipmentStatus next = request.getStatus();

        if (prev == next) {
            throw new InvalidShipmentTransitionException("Shipment is already in status: " + next);
        }

        shipment.setStatus(next);
        if (request.getTrackingNumber() != null) shipment.setTrackingNumber(request.getTrackingNumber());

        Instant now = Instant.now();
        applyStatusTimestamps(shipment, next, now);

        shipmentRepository.save(shipment);
        recordEvent(shipment, next, request.getNotes(), request.getLocation());

        if (next == ShipmentStatus.DELIVERED) {
            eventPublisher.publish(new ShipmentDeliveredEvent(
                    shipment.getPublicId(), shipment.getShipmentNumber(),
                    shipment.getFulfillment().getOrderId(), sellerId, LocalDate.now()));
        }

        eventPublisher.publish(new ShipmentStatusChangedEvent(
                shipment.getPublicId(), shipment.getShipmentNumber(),
                shipment.getFulfillment().getOrderId(), sellerId,
                prev, next, shipment.getTrackingNumber()));

        log.info("Shipment {} status {} -> {}", shipment.getShipmentNumber(), prev, next);
        return toResponse(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse generateLabel(UUID userId, UUID shipmentPublicId) {
        Long sellerId = resolveSellerLongId(userId);
        Shipment shipment = findOrThrow(shipmentPublicId);
        assertSellerOwns(shipment, sellerId);

        ShipmentAddress addr = shipment.getAddress();
        ShipmentProviderRequest providerRequest = ShipmentProviderRequest.builder()
                .recipientName(addr != null ? addr.getRecipientName() : null)
                .recipientPhone(addr != null ? addr.getRecipientPhone() : null)
                .addressLine1(addr != null ? addr.getAddressLine1() : null)
                .city(addr != null ? addr.getCity() : null)
                .country(addr != null ? addr.getCountry() : "Ghana")
                .weightKg(shipment.getWeightKg())
                .serviceCode(shipment.getShippingMethod() != null ? shipment.getShippingMethod().getCode() : "STANDARD")
                .build();

        ShipmentProviderResult result = shippingProvider.createShipment(providerRequest);

        shipment.setTrackingNumber(result.getTrackingNumber());
        shipment.setLabelUrl(result.getLabelUrl());
        shipment.setStatus(ShipmentStatus.LABEL_CREATED);
        shipment.setLabelCreatedAt(Instant.now());
        if (result.getEstimatedDeliveryDate() != null) {
            shipment.setEstimatedDeliveryDate(result.getEstimatedDeliveryDate());
        }
        if (result.getCost() != null) {
            shipment.setShippingCost(result.getCost());
        }
        shipmentRepository.save(shipment);

        ShippingLabel label = ShippingLabel.builder()
                .shipment(shipment)
                .labelUrl(result.getLabelUrl())
                .carrierLabelId(result.getCarrierLabelId())
                .build();
        shippingLabelRepository.save(label);

        recordEvent(shipment, ShipmentStatus.LABEL_CREATED, "Shipping label generated", null);
        log.info("Generated label for shipment={}, tracking={}", shipment.getShipmentNumber(), result.getTrackingNumber());
        return toResponse(shipment);
    }

    @Override
    @Transactional
    public TrackingEventResponse recordTrackingEvent(UUID shipmentPublicId, RecordTrackingEventRequest request) {
        Shipment shipment = findOrThrow(shipmentPublicId);
        TrackingEvent event = recordEvent(shipment, request.getStatus(), request.getDescription(), request.getLocation());
        if (request.getOccurredAt() != null) {
            event.setOccurredAt(request.getOccurredAt());
            event = trackingEventRepository.save(event);
        }
        return TrackingEventResponse.from(event);
    }

    @Override
    @Transactional
    public void cancelShipment(UUID userId, UUID shipmentPublicId) {
        Long sellerId = resolveSellerLongId(userId);
        Shipment shipment = findOrThrow(shipmentPublicId);
        assertSellerOwns(shipment, sellerId);

        if (!CANCELLABLE_STATUSES.contains(shipment.getStatus())) {
            throw new InvalidShipmentTransitionException(
                    "Shipment cannot be cancelled in status: " + shipment.getStatus());
        }

        if (shipment.getTrackingNumber() != null) {
            shippingProvider.cancelShipment(shipment.getTrackingNumber());
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancelledAt(Instant.now());
        shipmentRepository.save(shipment);
        recordEvent(shipment, ShipmentStatus.CANCELLED, "Shipment cancelled by seller", null);
        log.info("Cancelled shipment={}", shipment.getShipmentNumber());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Shipment findOrThrow(UUID publicId) {
        return shipmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + publicId));
    }

    private void assertSellerOwns(Shipment shipment, Long sellerId) {
        if (!shipment.getFulfillment().getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You do not own this shipment");
        }
    }

    private ShipmentAddress buildAddress(CreateShipmentRequest request) {
        if (request.getAddress() == null) return null;
        return ShipmentAddress.builder()
                .recipientName(request.getAddress().getRecipientName())
                .recipientPhone(request.getAddress().getRecipientPhone())
                .addressLine1(request.getAddress().getAddressLine1())
                .addressLine2(request.getAddress().getAddressLine2())
                .city(request.getAddress().getCity())
                .state(request.getAddress().getState())
                .country(request.getAddress().getCountry() != null ? request.getAddress().getCountry() : "Ghana")
                .postalCode(request.getAddress().getPostalCode())
                .build();
    }

    private List<ShipmentItem> saveItems(Shipment shipment, CreateShipmentRequest request) {
        return request.getItems().stream().map(itemReq -> {
            ShipmentItem item = ShipmentItem.builder()
                    .shipment(shipment)
                    .orderItemId(itemReq.getOrderItemId())
                    .productId(itemReq.getProductId())
                    .variantId(itemReq.getVariantId())
                    .productName(itemReq.getProductName())
                    .productSku(itemReq.getProductSku())
                    .quantity(itemReq.getQuantity())
                    .build();
            return shipmentItemRepository.save(item);
        }).toList();
    }

    private TrackingEvent recordEvent(Shipment shipment, ShipmentStatus status, String description, String location) {
        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .status(status)
                .description(description)
                .location(location)
                .occurredAt(Instant.now())
                .build();
        return trackingEventRepository.save(event);
    }

    private void applyStatusTimestamps(Shipment shipment, ShipmentStatus status, Instant now) {
        switch (status) {
            case LABEL_CREATED -> shipment.setLabelCreatedAt(now);
            case PICKED_UP -> shipment.setPickedUpAt(now);
            case DELIVERED -> {
                shipment.setDeliveredAt(now);
                shipment.setActualDeliveryDate(LocalDate.now());
            }
            case CANCELLED -> shipment.setCancelledAt(now);
            default -> { /* other statuses don't have dedicated timestamp columns */ }
        }
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        List<ShipmentItemResponse> items = shipmentItemRepository
                .findByShipment_PublicId(shipment.getPublicId())
                .stream().map(ShipmentItemResponse::from).toList();
        List<TrackingEventResponse> events = trackingEventRepository
                .findByShipment_PublicIdOrderByOccurredAtDesc(shipment.getPublicId())
                .stream().map(TrackingEventResponse::from).toList();
        return ShipmentResponse.from(shipment, items, events);
    }

    private ShipmentResponse toResponse(Shipment shipment, List<ShipmentItem> items, List<TrackingEventResponse> events) {
        return ShipmentResponse.from(
                shipment,
                items.stream().map(ShipmentItemResponse::from).toList(),
                events);
    }

    private String generateShipmentNumber() {
        int year = java.time.Year.now().getValue();
        for (int attempt = 0; attempt < 10; attempt++) {
            long nano = System.nanoTime() % 1_000_000L;
            String candidate = String.format("SHP-%d-%06d", year, Math.abs(nano));
            if (!shipmentRepository.existsByShipmentNumber(candidate)) return candidate;
        }
        return "SHP-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public List<ShipmentResponse> getShipmentsByFulfillment(UUID fulfillmentPublicId) {
        return shipmentRepository.findByFulfillment_PublicId(fulfillmentPublicId)
                .stream().map(this::toResponse).toList();
    }

    private Long resolveSellerLongId(UUID userId) {
        return sellerRepository.findByOwnerUserId(userId)
                .map(s -> s.getId())
                .orElseThrow(() -> new ForbiddenException("User is not a registered seller"));
    }
}

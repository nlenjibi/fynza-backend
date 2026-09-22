package ecommerce.modules.shipping.event;

import ecommerce.common.event.DomainEvent;
import ecommerce.modules.shipping.enums.ShipmentStatus;

import java.util.UUID;

public record ShipmentStatusChangedEvent(
        UUID shipmentId,
        String shipmentNumber,
        UUID orderId,
        Long sellerId,
        ShipmentStatus previousStatus,
        ShipmentStatus newStatus,
        String trackingNumber
) implements DomainEvent {}

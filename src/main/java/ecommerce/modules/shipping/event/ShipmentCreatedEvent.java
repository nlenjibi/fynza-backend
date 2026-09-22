package ecommerce.modules.shipping.event;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

public record ShipmentCreatedEvent(
        UUID shipmentId,
        String shipmentNumber,
        UUID fulfillmentId,
        UUID orderId,
        Long sellerId
) implements DomainEvent {}

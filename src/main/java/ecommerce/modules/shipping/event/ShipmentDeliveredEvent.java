package ecommerce.modules.shipping.event;

import ecommerce.common.event.DomainEvent;

import java.time.LocalDate;
import java.util.UUID;

public record ShipmentDeliveredEvent(
        UUID shipmentId,
        String shipmentNumber,
        UUID orderId,
        Long sellerId,
        LocalDate deliveredDate
) implements DomainEvent {}

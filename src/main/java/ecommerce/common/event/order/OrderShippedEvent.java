package ecommerce.common.event.order;

import ecommerce.common.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published when an order's tracking number is set and status moves to SHIPPED.
 * Consumed to: send shipment notification with tracking details to customer.
 */
public record OrderShippedEvent(
        Long orderId,
        UUID publicOrderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        String trackingNumber,
        String carrierName,
        LocalDateTime estimatedDelivery
) implements DomainEvent {}

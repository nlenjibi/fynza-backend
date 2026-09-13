package ecommerce.common.event.order;

import ecommerce.common.event.DomainEvent;
import ecommerce.common.enums.OrderStatus;

import java.util.UUID;

/**
 * Published whenever an order transitions between statuses.
 * Consumed to: send status-update notification to customer.
 */
public record OrderStatusChangedEvent(
        Long orderId,
        UUID publicOrderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        OrderStatus previousStatus,
        OrderStatus newStatus
) implements DomainEvent {}

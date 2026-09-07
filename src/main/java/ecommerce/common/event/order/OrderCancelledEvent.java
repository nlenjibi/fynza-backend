package ecommerce.common.event.order;

import ecommerce.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when an order is cancelled.
 * Consumed to: release reserved stock, trigger refund if already paid,
 * notify customer and affected sellers.
 */
public record OrderCancelledEvent(
        Long orderId,
        UUID publicOrderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        BigDecimal totalAmount,
        String cancellationReason,
        boolean refundRequired
) implements DomainEvent {}

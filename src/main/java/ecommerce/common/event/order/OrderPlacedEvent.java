package ecommerce.common.event.order;

import ecommerce.common.event.DomainEvent;
import ecommerce.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published after a new order is successfully persisted.
 * Consumed to: send order confirmation email, notify seller(s), reserve stock.
 *
 * @param orderId       internal BIGINT PK (for service-to-service calls)
 * @param publicOrderId UUIDv7 (for email deep-links and API references)
 * @param orderNumber   human-readable order number (e.g. FYN-2026-001847)
 * @param customerId    internal BIGINT PK of the customer
 * @param customerEmail used by the notification listener as the send-to address
 * @param customerName  full name for email greeting
 * @param totalAmount   total charged to the customer
 * @param paymentMethod payment channel used
 */
public record OrderPlacedEvent(
        Long orderId,
        UUID publicOrderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod
) implements DomainEvent {}

package ecommerce.common.event.payment;

import ecommerce.common.event.DomainEvent;
import ecommerce.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a payment attempt is declined or errors out.
 * Consumed to: notify customer of failure, optionally release reserved stock
 * after a configurable retry window.
 */
public record PaymentFailedEvent(
        Long paymentId,
        UUID publicPaymentId,
        Long orderId,
        UUID publicOrderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String failureReason
) implements DomainEvent {}

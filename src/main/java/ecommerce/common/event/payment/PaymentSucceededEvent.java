package ecommerce.common.event.payment;

import ecommerce.common.event.DomainEvent;
import ecommerce.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published after a payment gateway confirms successful charge.
 * Consumed to: mark order as PAID, trigger fulfilment workflow, send receipt email.
 */
public record PaymentSucceededEvent(
        Long paymentId,
        UUID publicPaymentId,
        Long orderId,
        UUID publicOrderId,
        String orderNumber,
        Long customerId,
        String customerEmail,
        String customerName,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String providerReference
) implements DomainEvent {}

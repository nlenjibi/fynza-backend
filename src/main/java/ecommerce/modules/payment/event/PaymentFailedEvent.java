package ecommerce.modules.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(
    UUID transactionId,
    UUID orderId,
    String orderNumber,
    UUID customerId,
    String customerEmail,
    String customerFirstName,
    BigDecimal amount,
    String failureReason
) {}

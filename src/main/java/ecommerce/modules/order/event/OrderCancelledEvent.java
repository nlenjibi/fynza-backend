package ecommerce.modules.order.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCancelledEvent(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    String customerEmail,
    String customerFirstName,
    BigDecimal totalAmount,
    String cancellationReason,
    UUID sellerId
) {}

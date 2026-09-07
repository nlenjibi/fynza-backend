package ecommerce.modules.order.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedEvent(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    String customerEmail,
    String customerFirstName,
    BigDecimal totalAmount,
    UUID sellerId
) {}

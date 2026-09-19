package ecommerce.modules.order.event;

import java.util.UUID;

public record OrderCancelledEvent(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    String reason
) {}

package ecommerce.modules.order.event;

import ecommerce.common.enums.OrderStatus;
import java.util.UUID;

public record OrderStatusChangedEvent(
    UUID orderId,
    String orderNumber,
    UUID customerId,
    OrderStatus previousStatus,
    OrderStatus newStatus
) {}

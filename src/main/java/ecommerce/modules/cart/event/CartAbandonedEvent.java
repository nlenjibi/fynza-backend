package ecommerce.modules.cart.event;

import java.math.BigDecimal;
import java.util.UUID;

public record CartAbandonedEvent(
    UUID cartId,
    UUID customerId,
    String customerEmail,
    String customerFirstName,
    BigDecimal cartValue,
    int itemCount
) {}

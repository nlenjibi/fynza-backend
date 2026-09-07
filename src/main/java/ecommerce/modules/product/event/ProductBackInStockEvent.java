package ecommerce.modules.product.event;

import java.util.UUID;

public record ProductBackInStockEvent(
    UUID productId,
    String productName,
    UUID sellerId,
    int newStock
) {}

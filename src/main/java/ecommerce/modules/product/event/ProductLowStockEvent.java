package ecommerce.modules.product.event;

import java.util.UUID;

public record ProductLowStockEvent(
    UUID productId,
    String productName,
    UUID sellerId,
    String sellerEmail,
    int currentStock
) {}

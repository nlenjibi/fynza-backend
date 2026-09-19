package ecommerce.modules.inventory.event;

import java.util.UUID;

public record OutOfStockEvent(UUID inventoryPublicId, UUID productId) {}

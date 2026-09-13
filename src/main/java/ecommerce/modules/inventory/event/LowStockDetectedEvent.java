package ecommerce.modules.inventory.event;

import java.util.UUID;

public record LowStockDetectedEvent(UUID inventoryPublicId, int currentStock, int threshold) {}

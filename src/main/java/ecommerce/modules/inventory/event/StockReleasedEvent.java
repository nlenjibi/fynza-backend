package ecommerce.modules.inventory.event;

import java.util.UUID;

public record StockReleasedEvent(UUID inventoryPublicId, int quantity, UUID orderId) {}

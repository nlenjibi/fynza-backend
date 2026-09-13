package ecommerce.modules.inventory.event;

import java.util.UUID;

public record StockReservedEvent(UUID inventoryPublicId, int quantity, UUID orderId) {}

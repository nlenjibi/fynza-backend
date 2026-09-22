package ecommerce.modules.inventory.event;

import ecommerce.modules.inventory.enums.MovementType;

import java.util.UUID;

public record StockAdjustedEvent(UUID inventoryPublicId, int delta, MovementType type) {}

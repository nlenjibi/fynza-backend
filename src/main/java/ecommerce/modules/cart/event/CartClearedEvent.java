package ecommerce.modules.cart.event;

import java.util.UUID;

public record CartClearedEvent(UUID cartPublicId, UUID userId, int itemsCleared) {}

package ecommerce.modules.cart.event;

import java.util.UUID;

public record CartItemUpdatedEvent(UUID cartPublicId, UUID userId, UUID productId, UUID variantId, int oldQuantity, int newQuantity) {}

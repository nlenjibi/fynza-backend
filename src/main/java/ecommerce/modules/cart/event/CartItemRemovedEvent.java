package ecommerce.modules.cart.event;

import java.util.UUID;

public record CartItemRemovedEvent(UUID cartPublicId, UUID userId, UUID productId, UUID variantId, int quantity) {}

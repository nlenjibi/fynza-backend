package ecommerce.modules.cart.event;

import java.util.UUID;

public record CartMergedEvent(UUID userCartPublicId, UUID userId, String guestCartToken, int itemsMerged) {}

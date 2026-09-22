package ecommerce.modules.cart.event;

import java.util.UUID;

public record CouponRemovedEvent(UUID cartPublicId, UUID userId, String removedCouponCode) {}

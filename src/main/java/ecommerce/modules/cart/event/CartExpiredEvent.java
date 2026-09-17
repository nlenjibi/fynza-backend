package ecommerce.modules.cart.event;

import java.util.UUID;

public record CartExpiredEvent(UUID cartPublicId, UUID userId, boolean isGuest) {}

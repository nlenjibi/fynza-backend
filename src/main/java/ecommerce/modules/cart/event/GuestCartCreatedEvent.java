package ecommerce.modules.cart.event;

import java.util.UUID;

public record GuestCartCreatedEvent(UUID cartPublicId, String cartToken) {}

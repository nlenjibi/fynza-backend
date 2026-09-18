package ecommerce.modules.wishlist.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class WishlistCreatedEvent {

    private final UUID wishlistId;
    private final UUID customerId;
    private final String name;

    public WishlistCreatedEvent(UUID wishlistId, UUID customerId, String name) {
        this.wishlistId = wishlistId;
        this.customerId = customerId;
        this.name = name;
    }
}

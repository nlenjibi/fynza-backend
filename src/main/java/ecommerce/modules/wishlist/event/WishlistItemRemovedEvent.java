package ecommerce.modules.wishlist.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class WishlistItemRemovedEvent {

    private final UUID wishlistId;
    private final UUID customerId;
    private final UUID itemId;

    public WishlistItemRemovedEvent(UUID wishlistId, UUID customerId, UUID itemId) {
        this.wishlistId = wishlistId;
        this.customerId = customerId;
        this.itemId = itemId;
    }
}

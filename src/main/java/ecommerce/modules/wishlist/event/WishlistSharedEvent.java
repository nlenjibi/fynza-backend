package ecommerce.modules.wishlist.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class WishlistSharedEvent {

    private final UUID wishlistId;
    private final UUID customerId;

    public WishlistSharedEvent(UUID wishlistId, UUID customerId) {
        this.wishlistId = wishlistId;
        this.customerId = customerId;
    }
}

package ecommerce.modules.wishlist.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class WishlistMergedEvent {

    private final UUID customerId;
    private final int mergedCount;
    private final UUID guestWishlistId;

    public WishlistMergedEvent(UUID customerId, int mergedCount, UUID guestWishlistId) {
        this.customerId = customerId;
        this.mergedCount = mergedCount;
        this.guestWishlistId = guestWishlistId;
    }
}

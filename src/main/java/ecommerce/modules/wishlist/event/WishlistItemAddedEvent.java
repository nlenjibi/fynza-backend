package ecommerce.modules.wishlist.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class WishlistItemAddedEvent {

    private final UUID wishlistId;
    private final UUID customerId;
    private final UUID productId;
    private final UUID variantId;

    public WishlistItemAddedEvent(UUID wishlistId, UUID customerId, UUID productId, UUID variantId) {
        this.wishlistId = wishlistId;
        this.customerId = customerId;
        this.productId = productId;
        this.variantId = variantId;
    }
}

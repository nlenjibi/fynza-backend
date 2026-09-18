package ecommerce.modules.wishlist.service;

import ecommerce.modules.wishlist.entity.Wishlist;
import ecommerce.modules.wishlist.entity.WishlistItem;
import ecommerce.modules.wishlist.exception.WishlistAccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WishlistPolicy {

    public void assertOwner(UUID customerId, Wishlist wishlist) {
        if (!customerId.equals(wishlist.getCustomerId())) {
            throw new WishlistAccessDeniedException();
        }
    }

    public void assertItemOwner(UUID customerId, WishlistItem item) {
        assertOwner(customerId, item.getWishlist());
    }
}

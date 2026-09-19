package ecommerce.modules.wishlist.service;

import ecommerce.modules.wishlist.dto.request.*;
import ecommerce.modules.wishlist.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface WishlistService {

    WishlistResponse createWishlist(UUID customerId, CreateWishlistRequest request);

    WishlistResponse getWishlist(UUID customerId, UUID wishlistPublicId);

    WishlistResponse updateWishlist(UUID customerId, UUID wishlistPublicId, UpdateWishlistRequest request);

    void deleteWishlist(UUID customerId, UUID wishlistPublicId);

    WishlistResponse getOrCreateDefaultWishlist(UUID customerId);

    WishlistItemResponse addItem(UUID customerId, UUID wishlistPublicId, AddWishlistItemRequest request);

    void removeItem(UUID customerId, UUID itemPublicId);

    void moveToCart(UUID customerId, UUID itemPublicId);

    void addToCart(UUID customerId, UUID itemPublicId);

    WishlistItemResponse updateNotificationPreferences(UUID customerId, UUID itemPublicId, NotificationPreferenceRequest request);

    GuestWishlistResponse createGuestWishlist();

    void mergeGuestWishlist(UUID customerId, String guestToken);

    String shareWishlist(UUID customerId, UUID wishlistPublicId);

    void unshareWishlist(UUID customerId, UUID wishlistPublicId);

    String regenerateShareToken(UUID customerId, UUID wishlistPublicId);

    WishlistResponse getSharedWishlist(String shareToken);

    List<WishlistResponse> getMyWishlists(UUID customerId);

    boolean isInWishlist(UUID customerId, UUID productId, UUID variantId);
}

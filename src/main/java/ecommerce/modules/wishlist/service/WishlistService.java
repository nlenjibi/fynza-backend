package ecommerce.modules.wishlist.service;

import ecommerce.modules.wishlist.dto.AddToWishlistRequest;
import ecommerce.modules.wishlist.dto.UpdateWishlistItemRequest;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.dto.WishlistSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface WishlistService {

    WishlistItemDto addToWishlist(UUID userId, AddToWishlistRequest request);

    List<WishlistItemDto> getUserWishlist(UUID userId);

    Page<WishlistItemDto> getUserWishlistPaginated(UUID userId, Pageable pageable);

    WishlistSummaryDto getWishlistSummary(UUID userId);

    void removeFromWishlist(UUID userId, UUID productId);

    WishlistItemDto updateWishlistItem(UUID userId, UUID productId, UpdateWishlistItemRequest request);

    boolean isInWishlist(UUID userId, UUID productId);

    void clearWishlist(UUID userId);

    List<WishlistItemDto> getItemsWithPriceDrops(UUID userId);

    WishlistItemDto markAsPurchased(UUID userId, UUID productId);

    void moveToCart(UUID userId, UUID productId);

    long getWishlistCount(UUID userId);
}

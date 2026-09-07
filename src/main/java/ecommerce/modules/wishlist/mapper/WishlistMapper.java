package ecommerce.modules.wishlist.mapper;

import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.entity.WishlistItem;

public interface WishlistMapper {
    WishlistItemDto toDto(WishlistItem item);
}

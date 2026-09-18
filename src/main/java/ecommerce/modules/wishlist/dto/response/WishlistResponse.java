package ecommerce.modules.wishlist.dto.response;

import ecommerce.modules.wishlist.entity.WishlistStatus;
import ecommerce.modules.wishlist.entity.WishlistVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WishlistResponse {

    private UUID id;
    private String name;
    private String description;
    private WishlistStatus status;
    private WishlistVisibility visibility;
    private Boolean isDefault;
    private int itemCount;
    private List<WishlistItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}

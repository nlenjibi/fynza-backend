package ecommerce.modules.wishlist.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WishlistItemResponse {

    private UUID id;
    private UUID wishlistId;
    private UUID productId;
    private UUID variantId;
    private Boolean notifyOnPriceDrop;
    private Boolean notifyOnRestock;
    private Instant createdAt;
}

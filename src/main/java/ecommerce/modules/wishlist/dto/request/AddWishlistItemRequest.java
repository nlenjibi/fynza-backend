package ecommerce.modules.wishlist.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddWishlistItemRequest {

    @NotNull
    private UUID productId;

    private UUID variantId;

    private Boolean notifyOnPriceDrop = false;

    private Boolean notifyOnRestock = false;
}

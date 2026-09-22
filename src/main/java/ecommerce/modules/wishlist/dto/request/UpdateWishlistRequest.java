package ecommerce.modules.wishlist.dto.request;

import ecommerce.modules.wishlist.entity.WishlistVisibility;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWishlistRequest {

    @Size(max = 100)
    private String name;

    private String description;

    private WishlistVisibility visibility;
}

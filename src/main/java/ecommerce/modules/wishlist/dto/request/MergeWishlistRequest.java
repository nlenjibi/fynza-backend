package ecommerce.modules.wishlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MergeWishlistRequest {

    @NotBlank
    private String guestToken;
}

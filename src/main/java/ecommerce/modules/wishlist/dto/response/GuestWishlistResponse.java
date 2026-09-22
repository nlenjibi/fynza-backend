package ecommerce.modules.wishlist.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class GuestWishlistResponse {

    private UUID wishlistId;
    private String guestToken;
}

package ecommerce.modules.wishlist.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WishlistSummaryResponse {

    private UUID customerId;
    private int totalWishlists;
    private int totalItems;
    private List<WishlistResponse> wishlists;
}

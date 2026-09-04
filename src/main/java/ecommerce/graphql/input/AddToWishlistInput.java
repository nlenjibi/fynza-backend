package ecommerce.graphql.input;

import ecommerce.modules.wishlist.entity.WishlistPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWishlistInput {
    private UUID productId;
    private WishlistPriority priority;
    private String notes;
    private Integer desiredQuantity;
    private BigDecimal targetPrice;
    private Boolean notifyOnPriceDrop;
    private Boolean notifyOnStock;
    private Boolean isPublic;
    private String collectionName;
}

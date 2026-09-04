package ecommerce.graphql.input;

import ecommerce.modules.wishlist.entity.WishlistPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWishlistItemInput {
    private WishlistPriority priority;
    private String notes;
    private Integer desiredQuantity;
    private BigDecimal targetPrice;
    private Boolean notifyOnPriceDrop;
    private Boolean notifyOnStock;
    private Boolean isPublic;
}

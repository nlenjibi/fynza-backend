package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWishlistItemInput {
    private Boolean notifyOnPriceDrop;
    private Boolean notifyOnRestock;
}

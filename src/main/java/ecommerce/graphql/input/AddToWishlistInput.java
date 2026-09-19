package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToWishlistInput {
    private UUID productId;
    private UUID variantId;
    private Boolean notifyOnPriceDrop;
    private Boolean notifyOnRestock;
}

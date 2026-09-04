package ecommerce.modules.cart.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemData implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID productId;
    private UUID variantId;
    private Integer quantity;
    private BigDecimal price;
    private String productName;
    private String productImage;
    private String size;
    private String color;
}

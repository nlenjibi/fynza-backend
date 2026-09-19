package ecommerce.modules.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartValidateResponse {

    private boolean valid;
    private List<CartItemValidationResult> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItemValidationResult {

        private UUID productId;
        private UUID variantId;
        private Integer quantity;
        private BigDecimal expectedPrice;
        private BigDecimal currentPrice;
        private boolean priceChanged;
        private String currency;
        private String message;
    }
}

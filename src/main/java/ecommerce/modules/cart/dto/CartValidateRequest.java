package ecommerce.modules.cart.dto;

import ecommerce.modules.pricing.enums.SupportedCurrency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CartValidateRequest {

    @NotEmpty(message = "items must not be empty")
    private List<CartValidateItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartValidateItem {

        @NotNull(message = "productId is required")
        private UUID productId;

        private UUID variantId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "currency is required")
        private SupportedCurrency currency;

        @NotNull(message = "expectedPrice is required")
        private BigDecimal expectedPrice;
    }
}

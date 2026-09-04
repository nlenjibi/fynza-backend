package ecommerce.modules.cart.dto;

import ecommerce.modules.product.dto.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private UUID id;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Boolean reserved;
    private LocalDateTime reservationExpiresAt;
}

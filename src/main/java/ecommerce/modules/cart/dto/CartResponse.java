package ecommerce.modules.cart.dto;

import ecommerce.modules.cart.entity.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private UUID id;
    private UUID userId;
    private String cartToken;
    private CartStatus status;
    private Boolean isGuest;
    private List<CartItemResponse> items;
    private String couponCode;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingTotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private Integer itemsCount;
    private Instant expiresAt;
    private Instant updatedAt;
    private Boolean hasPriceChanges;
}

package ecommerce.modules.cart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_cart_summary")
@Getter
@NoArgsConstructor
public class CartSummaryView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "cart_token")
    private String cartToken;

    @Column(name = "status")
    private String status;

    @Column(name = "is_guest")
    private Boolean isGuest;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "shipping_total")
    private BigDecimal shippingTotal;

    @Column(name = "tax_total")
    private BigDecimal taxTotal;

    @Column(name = "grand_total")
    private BigDecimal grandTotal;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "total_quantity")
    private Integer totalQuantity;
}

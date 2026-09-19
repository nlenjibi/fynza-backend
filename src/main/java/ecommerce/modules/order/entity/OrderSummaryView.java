package ecommerce.modules.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_order_summary")
@Getter
@NoArgsConstructor
public class OrderSummaryView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "status")
    private String status;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "tax")
    private BigDecimal tax;

    @Column(name = "shipping_cost")
    private BigDecimal shippingCost;

    @Column(name = "discount")
    private BigDecimal discount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "seller_count")
    private Integer sellerCount;
}

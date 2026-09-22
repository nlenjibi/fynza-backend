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
@Table(name = "v_order_detail")
@Getter
@NoArgsConstructor
public class OrderDetailView {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_public_id")
    private UUID orderPublicId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "order_subtotal")
    private BigDecimal orderSubtotal;

    @Column(name = "order_tax")
    private BigDecimal orderTax;

    @Column(name = "order_shipping_cost")
    private BigDecimal orderShippingCost;

    @Column(name = "order_discount")
    private BigDecimal orderDiscount;

    @Column(name = "order_total")
    private BigDecimal orderTotal;

    @Column(name = "shipping_name")
    private String shippingName;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "shipping_line1")
    private String shippingLine1;

    @Column(name = "shipping_line2")
    private String shippingLine2;

    @Column(name = "shipping_city")
    private String shippingCity;

    @Column(name = "shipping_state")
    private String shippingState;

    @Column(name = "shipping_postal")
    private String shippingPostal;

    @Column(name = "shipping_country")
    private String shippingCountry;

    @Column(name = "order_is_active")
    private Boolean orderIsActive;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "seller_order_id")
    private Long sellerOrderId;

    @Column(name = "seller_order_public_id")
    private UUID sellerOrderPublicId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "seller_order_status")
    private String sellerOrderStatus;

    @Column(name = "seller_subtotal")
    private BigDecimal sellerSubtotal;

    @Column(name = "seller_tracking_number")
    private String sellerTrackingNumber;

    @Column(name = "item_public_id")
    private UUID itemPublicId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_sku")
    private String productSku;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "item_subtotal")
    private BigDecimal itemSubtotal;
}

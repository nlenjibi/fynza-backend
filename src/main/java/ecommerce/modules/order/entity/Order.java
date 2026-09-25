package ecommerce.modules.order.entity;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer_id",    columnList = "customer_id"),
        @Index(name = "idx_orders_order_number",   columnList = "order_number", unique = true),
        @Index(name = "idx_orders_status",         columnList = "status"),
        @Index(name = "idx_orders_payment_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"sellerOrders", "orderItems"})
public class Order {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "id", insertable = false, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "shipping_name")),
            @AttributeOverride(name = "phone",      column = @Column(name = "shipping_phone")),
            @AttributeOverride(name = "line1",      column = @Column(name = "shipping_line1")),
            @AttributeOverride(name = "line2",      column = @Column(name = "shipping_line2")),
            @AttributeOverride(name = "city",       column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "state",      column = @Column(name = "shipping_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_postal")),
            @AttributeOverride(name = "country",    column = @Column(name = "shipping_country"))
    })
    private OrderAddress shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "billing_name")),
            @AttributeOverride(name = "phone",      column = @Column(name = "billing_phone")),
            @AttributeOverride(name = "line1",      column = @Column(name = "billing_line1")),
            @AttributeOverride(name = "line2",      column = @Column(name = "billing_line2")),
            @AttributeOverride(name = "city",       column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state",      column = @Column(name = "billing_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal")),
            @AttributeOverride(name = "country",    column = @Column(name = "billing_country"))
    })
    private OrderAddress billingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @Getter(lombok.AccessLevel.NONE)
    private List<SellerOrder> sellerOrders = new ArrayList<>();

    public List<SellerOrder> getSellerOrders() {
        return Collections.unmodifiableList(sellerOrders);
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @Getter(lombok.AccessLevel.NONE)
    private List<OrderItem> orderItems = new ArrayList<>();

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }
}

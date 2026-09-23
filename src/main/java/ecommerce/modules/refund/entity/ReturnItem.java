package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnItemCondition;
import ecommerce.modules.refund.enums.ReturnReason;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_items", indexes = {
        @Index(name = "idx_return_items_return_id",     columnList = "return_id"),
        @Index(name = "idx_return_items_order_item_id", columnList = "order_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 20)
    private ReturnItemCondition condition;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
    }
}

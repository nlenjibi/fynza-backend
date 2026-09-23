package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.RefundMethod;
import ecommerce.modules.refund.enums.ReturnPolicyScope;
import ecommerce.modules.refund.enums.ReturnShippingResponsibility;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_policies", indexes = {
        @Index(name = "idx_return_policies_scope",      columnList = "scope"),
        @Index(name = "idx_return_policies_store_id",   columnList = "store_id"),
        @Index(name = "idx_return_policies_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private ReturnPolicyScope scope;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "return_window_days", nullable = false)
    @Builder.Default
    private Integer returnWindowDays = 14;

    @Column(name = "is_returnable", nullable = false)
    @Builder.Default
    private Boolean isReturnable = true;

    @Column(name = "condition_required", nullable = false)
    @Builder.Default
    private Boolean conditionRequired = false;

    /** Comma-separated ReturnReason enum names; null = all reasons allowed */
    @Column(name = "eligible_reasons", columnDefinition = "TEXT")
    private String eligibleReasons;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_shipping_responsibility", length = 20)
    @Builder.Default
    private ReturnShippingResponsibility returnShippingResponsibility = ReturnShippingResponsibility.CUSTOMER;

    @Column(name = "restocking_fee_percent", precision = 5, scale = 2)
    private BigDecimal restockingFeePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", length = 30)
    @Builder.Default
    private RefundMethod refundMethod = RefundMethod.ORIGINAL_PAYMENT;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

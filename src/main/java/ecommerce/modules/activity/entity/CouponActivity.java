package ecommerce.modules.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_activities", indexes = {
    @Index(name = "idx_coupon_activity_user", columnList = "user_id"),
    @Index(name = "idx_coupon_activity_coupon", columnList = "coupon_id"),
    @Index(name = "idx_coupon_activity_seller", columnList = "seller_id"),
    @Index(name = "idx_coupon_activity_type", columnList = "activity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "coupon_id")
    private UUID couponId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "order_amount", precision = 10, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        publicId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ActivityType {
        COUPON_CREATED,
        COUPON_UPDATED,
        COUPON_DELETED,
        COUPON_ACTIVATED,
        COUPON_PAUSED,
        COUPON_VALIDATED,
        COUPON_APPLIED,
        COUPON_FAILED,
        COUPON_EXPIRED,
        COUPON_DUPLICATED,
        VIEWED,
        SEARCHED
    }
}

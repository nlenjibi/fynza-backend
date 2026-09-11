package ecommerce.modules.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_promotion_activities", indexes = {
    @Index(name = "idx_admin_promo_act_promo", columnList = "promotion_id"),
    @Index(name = "idx_admin_promo_act_type", columnList = "activity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPromotionActivity {

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

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "promotion_id")
    private UUID promotionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "promotion_name", length = 255)
    private String promotionName;

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

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
        PROMOTION_CREATED,
        PROMOTION_UPDATED,
        PROMOTION_DELETED,
        PROMOTION_APPROVED,
        PROMOTION_REJECTED,
        PROMOTION_ACTIVATED,
        PROMOTION_DEACTIVATED,
        COUPON_USED,
        PROMOTION_EXPIRED,
        ANALYTICS_VIEWED,
        REPORT_GENERATED
    }
}

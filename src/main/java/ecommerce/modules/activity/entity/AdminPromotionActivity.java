package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
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
@SuperBuilder
public class AdminPromotionActivity extends BaseEntity {

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

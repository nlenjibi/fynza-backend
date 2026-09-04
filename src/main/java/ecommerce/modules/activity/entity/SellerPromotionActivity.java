package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seller_promotion_activities", indexes = {
    @Index(name = "idx_seller_promo_act_promo", columnList = "promotion_id"),
    @Index(name = "idx_seller_promo_act_seller", columnList = "seller_id"),
    @Index(name = "idx_seller_promo_act_type", columnList = "activity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SellerPromotionActivity extends BaseEntity {

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "promotion_id")
    private UUID promotionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "promotion_name", length = 255)
    private String promotionName;

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
        PROMOTION_ACTIVATED,
        PROMOTION_DEACTIVATED,
        PROMOTION_STARTED,
        PROMOTION_EXPIRED,
        DISCOUNT_APPLIED,
        COUPON_USED,
        PROMOTION_VIEWED,
        PROMOTION_CLONED
    }
}

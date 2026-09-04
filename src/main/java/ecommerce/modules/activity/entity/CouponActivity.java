package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
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
@SuperBuilder
public class CouponActivity extends BaseEntity {

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

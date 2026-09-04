package ecommerce.modules.promotion.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_promotions", indexes = {
    @Index(name = "idx_seller_promo_seller", columnList = "seller_id"),
    @Index(name = "idx_seller_promo_status", columnList = "status"),
    @Index(name = "idx_seller_promo_type", columnList = "promotion_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SellerPromotion extends BaseEntity {

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "promotion_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PromotionType promotionType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_type", length = 20)
    @Builder.Default
    private String discountType = "PERCENTAGE";

    @Column(name = "min_purchase", precision = 10, scale = 2)
    private BigDecimal minPurchase;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.SCHEDULED;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public enum PromotionType {
        DISCOUNT,
        COUPON,
        FLASH_SALE
    }

    public enum PromotionStatus {
        ACTIVE,
        SCHEDULED,
        EXPIRED
    }
}

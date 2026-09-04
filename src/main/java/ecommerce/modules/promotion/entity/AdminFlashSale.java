package ecommerce.modules.promotion.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_flash_sales", indexes = {
    @Index(name = "idx_flash_sale_status", columnList = "status"),
    @Index(name = "idx_flash_sale_dates", columnList = "start_datetime,end_datetime")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminFlashSale extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "min_purchase_amount", precision = 10, scale = 2)
    private BigDecimal minPurchaseAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "max_products_per_seller")
    @Builder.Default
    private Integer maxProductsPerSeller = 10;

    @Column(name = "max_total_products")
    @Builder.Default
    private Integer maxTotalProducts = 100;

    @Column(name = "current_products_count")
    @Builder.Default
    private Integer currentProductsCount = 0;

    @Column(length = 100)
    private String category;

    @Column(name = "banner_image", length = 500)
    private String bannerImage;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SCHEDULED;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public int getSlotsRemaining() {
        return maxTotalProducts - currentProductsCount;
    }

    public enum Status {
        SCHEDULED,
        ACTIVE,
        EXPIRED
    }
}

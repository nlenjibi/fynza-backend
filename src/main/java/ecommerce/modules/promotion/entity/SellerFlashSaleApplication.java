package ecommerce.modules.promotion.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seller_flash_sale_applications", indexes = {
    @Index(name = "idx_seller_flash_sale_app_seller", columnList = "seller_id"),
    @Index(name = "idx_seller_flash_sale_app_flash_sale", columnList = "flash_sale_id"),
    @Index(name = "idx_seller_flash_sale_unique", columnList = "flash_sale_id,seller_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SellerFlashSaleApplication extends BaseEntity {

    @Column(name = "flash_sale_id", nullable = false)
    private UUID flashSaleId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private java.time.LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "orders_count")
    @Builder.Default
    private Integer ordersCount = 0;

    @Column(name = "revenue_generated", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal revenueGenerated = BigDecimal.ZERO;

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}

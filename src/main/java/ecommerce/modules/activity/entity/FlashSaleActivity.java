package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "flash_sale_activities", indexes = {
    @Index(name = "idx_flash_sale_activity_user", columnList = "user_id"),
    @Index(name = "idx_flash_sale_activity_seller", columnList = "seller_id"),
    @Index(name = "idx_flash_sale_activity_flash_sale", columnList = "flash_sale_id"),
    @Index(name = "idx_flash_sale_activity_type", columnList = "activity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FlashSaleActivity extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "flash_sale_id")
    private UUID flashSaleId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "flash_sale_name", length = 200)
    private String flashSaleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    public enum ActivityType {
        FLASH_SALE_CREATED,
        FLASH_SALE_UPDATED,
        FLASH_SALE_DELETED,
        SELLER_APPLIED,
        APPLICATION_APPROVED,
        APPLICATION_REJECTED,
        PRODUCT_ADDED,
        PRODUCT_REMOVED,
        FLASH_SALE_STARTED,
        FLASH_SALE_ENDED,
        FLASH_SALE_CANCELLED,
        VIEWED,
        SEARCHED
    }
}

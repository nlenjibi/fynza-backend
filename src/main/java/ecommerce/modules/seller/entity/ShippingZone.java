package ecommerce.modules.seller.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.modules.user.entity.SellerProfile;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_zones", indexes = {
    @Index(name = "idx_shipping_zone_seller", columnList = "seller_id"),
    @Index(name = "idx_shipping_zone_region", columnList = "region")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShippingZone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "zone_description", length = 255)
    private String zoneDescription;

    @Column(name = "region", length = 50)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", length = 30)
    @Builder.Default
    private DeliveryMethod deliveryMethod = DeliveryMethod.DIRECT_ADDRESS;

    @Column(name = "shipping_cost", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "free_shipping_min", precision = 10, scale = 2)
    private BigDecimal freeShippingMin;

    @Column(name = "estimated_days", length = 50)
    private String estimatedDays;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public enum DeliveryMethod {
        DIRECT_ADDRESS,
        BUS_STATION,
        SHIPPING
    }
}

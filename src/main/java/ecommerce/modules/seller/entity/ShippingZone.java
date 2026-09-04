package ecommerce.modules.seller.entity;

import ecommerce.modules.user.entity.SellerProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipping_zones", indexes = {
    @Index(name = "idx_shipping_zone_seller", columnList = "seller_id"),
    @Index(name = "idx_shipping_zone_region", columnList = "region")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

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

    public enum DeliveryMethod {
        DIRECT_ADDRESS,
        BUS_STATION,
        SHIPPING
    }
}

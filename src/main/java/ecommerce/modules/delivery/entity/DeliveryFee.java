package ecommerce.modules.delivery.entity;

import ecommerce.common.enums.DeliveryMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_fees", indexes = {
    @Index(name = "idx_delivery_town", columnList = "town_name"),
    @Index(name = "idx_delivery_region", columnList = "region_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "town_name", nullable = false, length = 100)
    private String townName;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 30)
    @Builder.Default
    private DeliveryMethod deliveryMethod = DeliveryMethod.DIRECT_ADDRESS;

    @Column(name = "base_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "per_km_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal perKmFee = BigDecimal.ZERO;

    @Column(name = "estimated_days", nullable = false)
    @Builder.Default
    private Integer estimatedDays = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private DeliveryRegion region;

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
}

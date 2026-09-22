package ecommerce.modules.shipping.entity;

import ecommerce.modules.pricing.enums.SupportedCurrency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipping_rates", indexes = {
        @Index(name = "idx_shipping_rates_method_id", columnList = "shipping_method_id"),
        @Index(name = "idx_shipping_rates_zone_id", columnList = "zone_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"shippingMethod", "zone"})
public class ShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id", nullable = false)
    private ShippingMethod shippingMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private ShippingZone zone;

    @Column(name = "base_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Column(name = "per_kg_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal perKgFee = BigDecimal.ZERO;

    @Column(name = "free_shipping_threshold", precision = 19, scale = 4)
    private BigDecimal freeShippingThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private SupportedCurrency currency = SupportedCurrency.GHS;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

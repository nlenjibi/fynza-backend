package ecommerce.modules.payout.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout_fee_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "payout_type", nullable = false, length = 30)
    private String payoutType;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "GHS";

    @Column(name = "flat_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal flatFee = BigDecimal.ZERO;

    @Column(name = "percentage_fee", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal percentageFee = BigDecimal.ZERO;

    @Column(name = "max_fee", precision = 19, scale = 4)
    private BigDecimal maxFee;

    @Column(name = "min_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minFee = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
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

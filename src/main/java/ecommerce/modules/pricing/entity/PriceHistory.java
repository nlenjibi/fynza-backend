package ecommerce.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_history", indexes = {
        @Index(name = "idx_price_history_price_id",   columnList = "price_id"),
        @Index(name = "idx_price_history_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_id", nullable = false)
    private Price price;

    @Column(name = "old_amount", precision = 19, scale = 4)
    private BigDecimal oldAmount;

    @Column(name = "new_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal newAmount;

    @Column(name = "old_currency", length = 3)
    private String oldCurrency;

    @Column(name = "new_currency", nullable = false, length = 3)
    private String newCurrency;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

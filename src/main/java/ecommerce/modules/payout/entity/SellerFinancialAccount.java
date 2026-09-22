package ecommerce.modules.payout.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_financial_accounts", indexes = {
        @Index(name = "idx_sfa_seller_id", columnList = "seller_id"),
        @Index(name = "idx_sfa_public_id",  columnList = "public_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerFinancialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "pending_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_paid_out", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalPaidOut = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

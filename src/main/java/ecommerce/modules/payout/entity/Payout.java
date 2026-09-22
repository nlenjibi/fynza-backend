package ecommerce.modules.payout.entity;

import ecommerce.modules.payout.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payouts", indexes = {
        @Index(name = "idx_payouts_seller_id",         columnList = "seller_id"),
        @Index(name = "idx_payouts_status",            columnList = "status"),
        @Index(name = "idx_payouts_payout_account_id", columnList = "payout_account_id"),
        @Index(name = "idx_payouts_financial_acct",    columnList = "financial_account_id"),
        @Index(name = "idx_payouts_idempotency_key",   columnList = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "payout_number", nullable = false, unique = true, length = 30)
    private String payoutNumber;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "payout_account_id", nullable = false)
    private Long payoutAccountId;

    @Column(name = "financial_account_id", nullable = false)
    private Long financialAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.REQUESTED;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "provider_reference", unique = true, length = 255)
    private String providerReference;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId    = UUID.randomUUID();
        requestedAt = Instant.now();
        createdAt   = Instant.now();
        updatedAt   = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_refunds", indexes = {
        @Index(name = "idx_return_refunds_return_id",  columnList = "return_id"),
        @Index(name = "idx_return_refunds_order_id",   columnList = "order_id"),
        @Index(name = "idx_return_refunds_status",     columnList = "status"),
        @Index(name = "idx_return_refunds_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    /** Public ID of the Return this refund belongs to (unique per return). */
    @Column(name = "return_id", nullable = false, unique = true)
    private UUID returnId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Public ID of the PaymentTransaction used for this refund. */
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "GHS";

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "provider_refund_id", length = 200)
    private String providerRefundId;

    @Column(name = "provider_reference", length = 200)
    private String providerReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

package ecommerce.modules.refund.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.common.enums.RefundReason;
import ecommerce.common.enums.RefundStatus;
import ecommerce.modules.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", indexes = {
        @Index(name = "idx_refund_order_id", columnList = "order_id"),
        @Index(name = "idx_refund_status", columnList = "status"),
        @Index(name = "idx_refund_customer_id", columnList = "customer_id"),
        @Index(name = "idx_refund_seller_id", columnList = "seller_id"),
        @Index(name = "idx_refund_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Refund extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(name = "refund_number", nullable = false, unique = true, length = 50)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "customer_id", nullable = false, columnDefinition = "UUID")
    private java.util.UUID customerId;

    @Column(name = "seller_id", columnDefinition = "UUID")
    private java.util.UUID sellerId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private RefundReason reason;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", columnDefinition = "UUID")
    private java.util.UUID reviewedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    public static String generateRefundNumber() {
        return "REF-" + System.currentTimeMillis();
    }
}

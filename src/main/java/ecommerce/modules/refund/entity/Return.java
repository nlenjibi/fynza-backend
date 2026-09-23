package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "returns", indexes = {
        @Index(name = "idx_returns_order_id",    columnList = "order_id"),
        @Index(name = "idx_returns_customer_id", columnList = "customer_id"),
        @Index(name = "idx_returns_seller_id",   columnList = "seller_id"),
        @Index(name = "idx_returns_status",      columnList = "status"),
        @Index(name = "idx_returns_created_at",  columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "return_number", nullable = false, unique = true, length = 30)
    private String returnNumber;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "store_id")
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReturnReason reason;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "return_deadline")
    private Instant returnDeadline;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    public static String generateReturnNumber() {
        return "RET-" + java.time.Year.now().getValue() + "-" +
               String.format("%06d", (long)(Math.random() * 1_000_000));
    }
}

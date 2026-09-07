package ecommerce.modules.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking payment-specific activities.
 *
 * Unlike the general ActivityLog which covers multiple entity types,
 * this entity is specifically designed for payment lifecycle tracking with
 * detailed payment information including amounts, methods, and status.
 */
@Entity
@Table(name = "payment_activity_logs", indexes = {
    @Index(name = "idx_payment_activity_log_user", columnList = "user_id"),
    @Index(name = "idx_payment_activity_log_payment", columnList = "payment_id"),
    @Index(name = "idx_payment_activity_log_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private PaymentActivityType activityType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

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

    /**
     * Enumeration of all possible payment activity types.
     * These represent the lifecycle events of a payment.
     */
    public enum PaymentActivityType {
        PAYMENT_INITIATED,
        PAYMENT_PENDING,
        PAYMENT_PROCESSING,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_CANCELLED,
        REFUND_INITIATED,
        REFUND_PROCESSING,
        REFUND_SUCCESS,
        REFUND_FAILED,
        PAYMENT_METHOD_ADDED,
        PAYMENT_METHOD_REMOVED,
        PAYMENT_METHOD_UPDATED,
        DEFAULT_METHOD_CHANGED,
        CARD_EXPIRY_WARNING,
        PAYMENT_RETRY,
        WALLET_FUNDED,
        WALLET_DEBITED
    }
}

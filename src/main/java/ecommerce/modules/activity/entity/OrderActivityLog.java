package ecommerce.modules.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking detailed order-specific activities.
 *
 * Unlike the general ActivityLog which covers multiple entity types,
 * this entity is specifically designed for order lifecycle tracking with
 * detailed status transitions and change tracking.
 */
@Entity
@Table(name = "order_activity_logs", indexes = {
    @Index(name = "idx_order_activity_log_order", columnList = "order_id"),
    @Index(name = "idx_order_activity_log_user", columnList = "user_id"),
    @Index(name = "idx_order_activity_log_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderActivityLog {

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

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private OrderActivityType activityType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

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
     * Enumeration of all possible order activity types.
     * These represent the lifecycle events of an order.
     */
    public enum OrderActivityType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        PAYMENT_RECEIVED,
        ORDER_PROCESSING,
        ORDER_SHIPPED,
        ORDER_IN_TRANSIT,
        ORDER_OUT_FOR_DELIVERY,
        ORDER_DELIVERED,
        ORDER_CANCELLED,
        REFUND_REQUESTED,
        REFUND_APPROVED,
        REFUND_REJECTED,
        REFUND_PROCESSED,
        TRACKING_UPDATED,
        ADDRESS_CHANGED,
        ITEM_REMOVED,
        ITEM_ADDED,
        QUANTITY_UPDATED,
        ORDER_NOTE_ADDED,
        DELIVERY_ATTEMPTED,
        DELIVERY_FAILED,
        DELIVERY_RESCHEDULED
    }
}

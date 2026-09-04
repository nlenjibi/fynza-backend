package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
public class OrderActivityLog extends BaseEntity {

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

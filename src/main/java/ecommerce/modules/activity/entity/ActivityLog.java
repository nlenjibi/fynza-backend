package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Unified entity for tracking all activities and audits across the platform.
 * 
 * This entity consolidates:
 * - General business activities (logins, orders, products)
 * - Technical audit logs (method calls, request details)
 * - User action tracking (CRUD operations, changes)
 * 
 * Table: activity_logs
 */
@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_user", columnList = "user_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_activity_created", columnList = "created_at"),
    @Index(name = "idx_activity_action", columnList = "action"),
    @Index(name = "idx_activity_status", columnList = "status"),
    @Index(name = "idx_activity_entity", columnList = "entity_type, entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ActivityLog extends BaseEntity {

    // =================================================================
    // USER IDENTIFICATION
    // =================================================================
    
    @Column(name = "user_id")
    private java.util.UUID userId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    // =================================================================
    // ACTIVITY/AUDIT TYPE
    // =================================================================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 50)
    private ActivityType activityType;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "status", length = 20)
    private String status;

    // =================================================================
    // BUSINESS ACTIVITY FIELDS
    // =================================================================
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    // =================================================================
    // ENTITY TRACKING
    // =================================================================
    
    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private java.util.UUID entityId;

    // =================================================================
    // TECHNICAL AUDIT FIELDS
    // =================================================================
    
    @Column(name = "method_name", length = 200)
    private String methodName;

    @Column(name = "class_name", length = 200)
    private String className;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    // =================================================================
    // REQUEST CONTEXT
    // =================================================================
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // =================================================================
    // ENUMS
    // =================================================================

    /**
     * Business activity types - represents high-level business events
     */
    public enum ActivityType {
        // User activities
        USER_LOGIN,
        USER_LOGOUT,
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        
        // Order activities
        ORDER_CREATED,
        ORDER_UPDATED,
        ORDER_CANCELLED,
        
        // Product activities
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED,
        
        // Category activities
        CATEGORY_CREATED,
        CATEGORY_UPDATED,
        CATEGORY_DELETED,
        
        // Review activities
        REVIEW_APPROVED,
        REVIEW_REJECTED,
        
        // Coupon activities
        COUPON_CREATED,
        COUPON_UPDATED,
        COUPON_DELETED,
        
        // Settings activities
        SETTINGS_UPDATED,
        
        // Payment activities
        PAYMENT_PROCESSED,
        REFUND_PROCESSED,
        DELIVERY_UPDATED,
        
        // Data operations
        EXPORT_DATA,
        IMPORT_DATA
    }

    /**
     * Audit action types - represents technical CRUD operations
     */
    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        READ,
        LOGIN,
        LOGOUT,
        CHANGE_PASSWORD,
        ROLE_CHANGE,
        STATUS_CHANGE,
        LOCK,
        UNLOCK
    }

    /**
     * Audit status - represents the outcome of an action
     */
    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        ATTEMPTED
    }
}

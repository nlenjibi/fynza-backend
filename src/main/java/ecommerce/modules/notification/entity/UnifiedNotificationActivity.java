package ecommerce.modules.notification.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unified_notification_activities", indexes = {
    @Index(name = "idx_unified_user", columnList = "user_id"),
    @Index(name = "idx_unified_type", columnList = "notification_type"),
    @Index(name = "idx_unified_category", columnList = "category"),
    @Index(name = "idx_unified_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UnifiedNotificationActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 30)
    private NotificationType notificationType;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_id")
    private UUID relatedId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "sender_type", length = 20)
    private String senderType;

    @Column(name = "sender_id")
    private UUID senderId;

    public enum ActivityType {
        SENT,
        RECEIVED,
        READ,
        CLICKED,
        DELETED,
        ARCHIVED,
        UNARCHIVED,
        MARKED_UNREAD,
        PINNED,
        UNPINNED,
        ALL_READ,
        ALL_DELETED,
        MESSAGE_SENT,
        MESSAGE_READ,
        CONVERSATION_STARTED,
        REPLY_SENT,
        FORWARDED,
        SETTINGS_CHANGED
    }

    public enum Category {
        NOTIFICATION,
        MESSAGE
    }

    public enum NotificationType {
        ORDER,
        PROMOTION,
        CART,
        SYSTEM,
        SELLER,
        SUPPORT
    }
}

package ecommerce.modules.notification.entity;

import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationStatus;
import ecommerce.modules.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit and retry record for a single outbound notification dispatch attempt.
 * One row is created per channel per notification event.
 * Status transitions: PENDING → SENDING → SENT or FAILED.
 */
@Entity
@Table(name = "notification_dispatch", indexes = {
    @Index(name = "idx_dispatch_recipient_id",  columnList = "recipient_id"),
    @Index(name = "idx_dispatch_status",        columnList = "status"),
    @Index(name = "idx_dispatch_event_id",      columnList = "notification_event_id"),
    @Index(name = "idx_dispatch_scheduled_at",  columnList = "scheduled_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    /** Nullable — for dispatches to external recipients who have no user account. */
    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "notification_event_id", nullable = false)
    private UUID notificationEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 80)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "subject", length = 300)
    private String subject;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "provider_name", length = 50)
    private String providerName;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /** Dedup key for Slack broadcast dispatches — the source entity's public UUID. */
    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    /** Slack channel ID stored at dispatch time so retries use the originally resolved channel. */
    @Column(name = "slack_channel_id", length = 32)
    private String slackChannelId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = Instant.now(); }
}

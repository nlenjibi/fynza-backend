package ecommerce.modules.notification.entity;

import ecommerce.modules.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-notification-type channel configuration controlling which delivery channels are active
 * and the retry policy applied when a dispatch attempt fails.
 * One row per NotificationType; missing rows fall back to service defaults (email + in-app, 3 retries, 60s delay).
 */
@Entity
@Table(name = "notification_channel_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, unique = true, length = 80)
    private NotificationType notificationType;

    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Builder.Default
    @Column(name = "slack_enabled", nullable = false)
    private boolean slackEnabled = false;

    @Builder.Default
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Builder.Default
    @Column(name = "retry_delay_seconds", nullable = false)
    private int retryDelaySeconds = 60;

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

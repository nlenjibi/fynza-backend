package ecommerce.modules.notification.entity;

import ecommerce.modules.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notif_recipient_id", columnList = "recipient_id"),
    @Index(name = "idx_notif_public_id",    columnList = "public_id"),
    @Index(name = "idx_notif_is_read",      columnList = "is_read"),
    @Index(name = "idx_notif_created_at",   columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    /** Nullable — set only for seller-context notifications. */
    @Column(name = "seller_id")
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 80)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "deep_link", length = 500)
    private String deepLink;

    @Column(name = "entity_type", length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
    }
}

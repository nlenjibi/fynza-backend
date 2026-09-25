package ecommerce.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_webhook_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_notif_webhook_provider_event",
        columnNames = {"provider", "provider_event_id"}
    ),
    indexes = @Index(name = "idx_notif_webhook_status", columnList = "status")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, length = 200)
    private String providerEventId;

    @Column(name = "event_type", length = 60)
    private String eventType;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "RECEIVED";

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    protected void onCreate() { receivedAt = Instant.now(); }
}

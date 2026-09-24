package ecommerce.modules.notification.entity;

import ecommerce.modules.notification.enums.DevicePlatform;
import ecommerce.modules.notification.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_devices", indexes = {
    @Index(name = "idx_notif_device_user_id", columnList = "user_id"),
    @Index(name = "idx_notif_device_status",  columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "device_token", nullable = false, unique = true, length = 500)
    private String deviceToken;

    @Column(name = "app_version", length = 30)
    private String appVersion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId    = UUID.randomUUID();
        createdAt   = updatedAt = Instant.now();
        lastSeenAt  = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = Instant.now(); }
}

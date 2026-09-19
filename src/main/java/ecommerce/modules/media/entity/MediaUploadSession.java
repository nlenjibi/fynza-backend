package ecommerce.modules.media.entity;

import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_upload_sessions", indexes = {
        @Index(name = "idx_upload_sessions_user_id",    columnList = "user_id"),
        @Index(name = "idx_upload_sessions_status",     columnList = "status"),
        @Index(name = "idx_upload_sessions_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "mediaAsset")
public class MediaUploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id")
    private MediaAsset mediaAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "expected_size", nullable = false)
    private Long expectedSize;

    @Column(name = "actual_size")
    private Long actualSize;

    @Column(name = "checksum")
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UploadSessionStatus status = UploadSessionStatus.CREATED;

    @Column(name = "upload_url")
    private String uploadUrl;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        if (status == null) status = UploadSessionStatus.CREATED;
    }
}

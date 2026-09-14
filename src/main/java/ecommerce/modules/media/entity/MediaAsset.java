package ecommerce.modules.media.entity;

import ecommerce.modules.media.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "media_assets", indexes = {
        @Index(name = "idx_media_assets_owner",       columnList = "owner_id, owner_type"),
        @Index(name = "idx_media_assets_status",      columnList = "status"),
        @Index(name = "idx_media_assets_media_type",  columnList = "media_type"),
        @Index(name = "idx_media_assets_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_media_assets_is_active",   columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "variants")
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 50)
    private MediaOwnerType ownerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "bucket")
    private String bucket;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", length = 500)
    private String storedFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 50)
    private MediaType mediaType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "etag")
    private String etag;

    @Column(name = "provider_asset_id", length = 500)
    private String providerAssetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private MediaVisibility visibility = MediaVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MediaStatus status = MediaStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_method", length = 30)
    private UploadMethod uploadMethod;

    @Column(name = "cdn_url")
    private String cdnUrl;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "mediaAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MediaVariant> variants = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status     == null) status     = MediaStatus.PENDING;
        if (visibility == null) visibility = MediaVisibility.PUBLIC;
        if (isActive   == null) isActive   = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

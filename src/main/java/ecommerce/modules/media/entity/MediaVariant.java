package ecommerce.modules.media.entity;

import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.VariantName;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "media_variants", indexes = {
        @Index(name = "idx_media_variants_asset_id", columnList = "media_asset_id"),
        @Index(name = "idx_media_variants_status",   columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "mediaAsset")
public class MediaVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_name", nullable = false, length = 50)
    private VariantName variantName;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "format", length = 20)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "url")
    private String url;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

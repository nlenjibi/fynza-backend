package ecommerce.modules.store.entity;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stores", indexes = {
        @Index(name = "idx_stores_public_id",  columnList = "public_id"),
        @Index(name = "idx_stores_seller_id",  columnList = "seller_id"),
        @Index(name = "idx_stores_slug",       columnList = "slug"),
        @Index(name = "idx_stores_status",     columnList = "status"),
        @Index(name = "idx_stores_visibility", columnList = "visibility"),
        @Index(name = "idx_stores_is_active",  columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "publicId", "slug", "status"})
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_media_id", length = 255)
    private String logoMediaId;

    @Column(name = "banner_media_id", length = 255)
    private String bannerMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StoreStatus status = StoreStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private StoreVisibility visibility = StoreVisibility.PRIVATE;

    @Column(name = "business_email", length = 255)
    private String businessEmail;

    @Column(name = "business_phone", length = 50)
    private String businessPhone;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status     == null) status     = StoreStatus.DRAFT;
        if (visibility == null) visibility = StoreVisibility.PRIVATE;
        if (isActive   == null) isActive   = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

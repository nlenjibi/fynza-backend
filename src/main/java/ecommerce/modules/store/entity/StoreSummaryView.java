package ecommerce.modules.store.entity;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_store_summary")
@Getter
@NoArgsConstructor
public class StoreSummaryView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "slug")
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "logo_media_id")
    private String logoMediaId;

    @Column(name = "banner_media_id")
    private String bannerMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StoreStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private StoreVisibility visibility;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "business_email")
    private String businessEmail;

    @Column(name = "business_phone")
    private String businessPhone;

    @Column(name = "website")
    private String website;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "seller_display_name")
    private String sellerDisplayName;

    @Column(name = "product_count")
    private Long productCount;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "review_count")
    private Long reviewCount;

    @Column(name = "order_count")
    private Long orderCount;
}

package ecommerce.modules.review.entity;

import ecommerce.modules.review.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_customer_id",            columnList = "customer_id"),
                @Index(name = "idx_reviews_product_status_created", columnList = "product_id, status, created_at"),
                @Index(name = "idx_reviews_seller_status_created",  columnList = "seller_id, status, created_at"),
                @Index(name = "idx_reviews_status_created",         columnList = "status, created_at"),
                @Index(name = "idx_reviews_order_item_id",          columnList = "order_item_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reviews_product_customer_order_item",
                        columnNames = {"product_id", "customer_id", "order_item_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Review {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "id", insertable = false, updatable = false)
    private UUID publicId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "order_item_id")
    private UUID orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING_MODERATION;

    @Column(name = "rating", columnDefinition = "SMALLINT")
    private Integer rating;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "verified_purchase", nullable = false)
    @Builder.Default
    private Boolean verifiedPurchase = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean canBeEditedBy(UUID userId) {
        return customerId.equals(userId)
                && status != ReviewStatus.DELETED
                && status != ReviewStatus.REJECTED
                && status != ReviewStatus.HIDDEN;
    }

    public boolean canBeDeletedBy(UUID userId) {
        return customerId.equals(userId) && status != ReviewStatus.DELETED;
    }

    public boolean isOwnedBy(UUID userId) {
        return customerId.equals(userId);
    }

    public void markPublished() {
        this.status      = ReviewStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markDeleted() {
        this.status = ReviewStatus.DELETED;
    }

    public void markHidden() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void markFlagged() {
        this.status = ReviewStatus.FLAGGED;
    }

    public void markPendingModeration() {
        if (this.status != ReviewStatus.PUBLISHED) {
            this.publishedAt = null;
        }
        this.status = ReviewStatus.PENDING_MODERATION;
    }
}

package ecommerce.modules.review.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"),
        @Index(name = "idx_review_customer", columnList = "customer_id"),
        @Index(name = "idx_review_rating", columnList = "rating")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_customer", columnNames = {"product_id", "customer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "has_images")
    @Builder.Default
    private Boolean hasImages = false;

    @Column(name = "pros")
    private String pros;

    @Column(name = "cons")
    private String cons;

    @Column(name = "helpful")
    @Builder.Default
    private Integer helpful = 0;

    @Column(name = "unhelpful")
    @Builder.Default
    private Integer unhelpful = 0;

    @Column(name = "verified_purchase", nullable = false)
    @Builder.Default
    private Boolean verifiedPurchase = false;

    @Column(name = "approved")
    @Builder.Default
    private Boolean approved = false;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "admin_response_at")
    private LocalDateTime adminResponseAt;

    @Column(name = "admin_response_by")
    private UUID adminResponseBy;

    @Column(name = "seller_reply", columnDefinition = "TEXT")
    private String sellerReply;

    @Column(name = "seller_replied_at")
    private LocalDateTime sellerRepliedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean canBeEditedBy(UUID userId) {
        return customer.getId().equals(userId) && !Boolean.TRUE.equals(deleted);
    }

    public boolean canBeDeletedBy(UUID userId) {
        return customer.getId().equals(userId);
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }

    public void approve() {
        this.approved = true;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.approved = false;
        this.rejectionReason = reason;
    }

    public void incrementHelpful() {
        if (this.helpful == null) {
            this.helpful = 0;
        }
        this.helpful++;
    }

    public void incrementUnhelpful() {
        if (this.unhelpful == null) {
            this.unhelpful = 0;
        }
        this.unhelpful++;
    }

    public void addAdminResponse(String response, UUID adminId) {
        this.adminResponse = response;
        this.adminResponseAt = LocalDateTime.now();
        this.adminResponseBy = adminId;
    }
}

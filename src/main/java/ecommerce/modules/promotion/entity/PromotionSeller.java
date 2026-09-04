package ecommerce.modules.promotion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotion_participating_sellers", indexes = {
    @Index(name = "idx_participating_seller", columnList = "seller_id"),
    @Index(name = "idx_participating_promotion", columnList = "promotion_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionSeller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "joined_at")
    private java.time.LocalDateTime joinedAt;

    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";

    @PrePersist
    protected void onCreate() {
        publicId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

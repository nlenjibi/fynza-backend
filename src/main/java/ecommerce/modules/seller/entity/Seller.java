package ecommerce.modules.seller.entity;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.enums.SellerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sellers", indexes = {
        @Index(name = "idx_sellers_public_id",  columnList = "public_id"),
        @Index(name = "idx_sellers_number",     columnList = "seller_number"),
        @Index(name = "idx_sellers_owner",      columnList = "owner_user_id"),
        @Index(name = "idx_sellers_status",     columnList = "status"),
        @Index(name = "idx_sellers_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "seller_number", nullable = false, unique = true, updatable = false, length = 20)
    private String sellerNumber;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_type", nullable = false, length = 30)
    @Builder.Default
    private SellerType sellerType = SellerType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SellerStatus status = SellerStatus.DRAFT;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

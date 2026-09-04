package ecommerce.modules.promotion.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
public class PromotionSeller extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
}

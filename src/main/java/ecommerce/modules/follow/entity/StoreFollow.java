package ecommerce.modules.follow.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_follows", uniqueConstraints = {
    @UniqueConstraint(name = "uk_store_follow_customer_seller", columnNames = {"customer_id", "seller_id"})
}, indexes = {
    @Index(name = "idx_store_follow_customer", columnList = "customer_id"),
    @Index(name = "idx_store_follow_seller", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StoreFollow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;

    @Column(name = "followed_at", nullable = false)
    @Builder.Default
    private LocalDateTime followedAt = LocalDateTime.now();
}

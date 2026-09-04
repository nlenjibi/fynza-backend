package ecommerce.modules.wishlist.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items", indexes = {
        @Index(name = "idx_wishlist_item_user", columnList = "user_id"),
        @Index(name = "idx_wishlist_item_product", columnList = "product_id"),
        @Index(name = "idx_wishlist_item_wishlist", columnList = "wishlist_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wishlist_id", nullable = false)
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private WishlistPriority priority = WishlistPriority.MEDIUM;

    @Column(length = 1000)
    private String notes;

    @Column(name = "desired_quantity")
    @Builder.Default
    private Integer desiredQuantity = 1;

    @Column(name = "target_price", precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "notify_on_price_drop")
    @Builder.Default
    private Boolean notifyOnPriceDrop = true;

    @Column(name = "notify_on_stock")
    @Builder.Default
    private Boolean notifyOnStock = true;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "purchased")
    @Builder.Default
    private Boolean purchased = false;

    @Column(name = "price_dropped")
    @Builder.Default
    private Boolean priceDropped = false;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    public void markAsPurchased() {
        this.purchased = true;
        this.purchasedAt = LocalDateTime.now();
    }

    public boolean isPriceDropped() {
        return Boolean.TRUE.equals(this.priceDropped);
    }
}

package ecommerce.modules.product.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.common.enums.InventoryStatus;
import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products", indexes = {
    @Index(name = "idx_product_seller_id", columnList = "seller_id"),
    @Index(name = "idx_product_category_id", columnList = "category_id"),
    @Index(name = "idx_product_status", columnList = "status"),
    @Index(name = "idx_product_price", columnList = "price"),
    @Index(name = "idx_product_rating", columnList = "rating"),
    @Index(name = "idx_product_brand", columnList = "brand")
})
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String brand;

    @Column
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column
    private BigDecimal originalPrice;

    @Column
    private BigDecimal discount;

    @Column
    private Integer stock;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer soldQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column
    private Integer reviewCount;

    @Column
    @Builder.Default
    private Integer viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InventoryStatus inventoryStatus = InventoryStatus.IN_STOCK;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isNew = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isBestseller = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Column
    private String mainImageUrl;

    public void addImage(ProductImage image) {
        images.add(image);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
    }

    public boolean isInStock() {
        return (stock != null && stock > 0) || (availableQuantity != null && availableQuantity > 0);
    }

    public void addStock(int quantity) {
        this.stock = (this.stock != null ? this.stock : 0) + quantity;
        this.availableQuantity = (this.availableQuantity != null ? this.availableQuantity : 0) + quantity;
    }

    public void releaseReservedStock(int quantity) {
        if (availableQuantity != null) {
            this.availableQuantity += quantity;
        }
        if (stock != null) {
            this.stock += quantity;
        }
    }

    public void reserveStock(int quantity) {
        if (availableQuantity != null) {
            this.availableQuantity -= quantity;
        }
        if (stock != null) {
            this.stock -= quantity;
        }
    }

    public void reduceStock(int quantity) {
        this.stock = (this.stock != null ? this.stock : 0) - quantity;
        this.availableQuantity = (this.availableQuantity != null ? this.availableQuantity : 0) - quantity;
    }

    public String getImageUrl() {
        if (mainImageUrl != null && !mainImageUrl.isEmpty()) {
            return mainImageUrl;
        }
        return (images != null && !images.isEmpty()) ? images.get(0).getImageUrl() : null;
    }

    public InventoryStatus getInventoryStatus() {
        int currentStock = (stock != null) ? stock : (availableQuantity != null ? availableQuantity : 0);
        if (currentStock <= 0) {
            return InventoryStatus.OUT_OF_STOCK;
        } else if (currentStock < 10) {
            return InventoryStatus.LOW_STOCK;
        } else {
            return InventoryStatus.IN_STOCK;
        }
    }
}

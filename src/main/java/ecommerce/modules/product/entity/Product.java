package ecommerce.modules.product.entity;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.enums.ProductType;
import ecommerce.common.enums.ProductVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products", indexes = {
    @Index(name = "idx_products_product_number", columnList = "product_number"),
    @Index(name = "idx_products_store_id",       columnList = "store_id"),
    @Index(name = "idx_products_seller_id",      columnList = "seller_id"),
    @Index(name = "idx_products_status",         columnList = "status"),
    @Index(name = "idx_products_visibility",     columnList = "visibility"),
    @Index(name = "idx_products_created_at",     columnList = "created_at")
})
public class Product {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt  = Instant.now();
        updatedAt  = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Column(name = "product_number", unique = true)
    private String productNumber;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sku")
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    @Builder.Default
    private ProductType productType = ProductType.SIMPLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private ProductVisibility visibility = ProductVisibility.PRIVATE;
}

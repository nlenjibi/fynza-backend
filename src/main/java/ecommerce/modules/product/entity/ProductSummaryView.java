package ecommerce.modules.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_product_summary")
@Getter
@NoArgsConstructor
public class ProductSummaryView {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "product_number")
    private String productNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sku")
    private String sku;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "visibility")
    private String visibility;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "category_slug")
    private String categorySlug;

    @Column(name = "on_hand_quantity")
    private Integer onHandQuantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "computed_inventory_status")
    private String computedInventoryStatus;
}

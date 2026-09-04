package ecommerce.modules.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String brand;
    private String sku;
    private CategoryInfo category;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discount;
    private BigDecimal discountPrice;
    private BigDecimal costPrice;
    private BigDecimal effectivePrice;
    private BigDecimal discountPercentage;
    private Integer stock;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer lowStockThreshold;
    private Integer reorderPoint;
    private Integer reorderQuantity;
    private Integer maxStockQuantity;
    private BigDecimal rating;
    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Integer ratingCount;
    private Integer viewCount;
    private Integer soldQuantity;
    private Long salesCount;
    private List<String> images;
    private List<ProductVariantResponse> variants;
    private Boolean inStock;
    private Integer stockCount;
    private Boolean trackInventory;
    private Boolean allowBackorder;
    private Boolean featured;
    private Boolean isNew;
    private Boolean isBestseller;
    private SellerInfo seller;
    private String status;
    private Boolean isApproved;
    private String inventoryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}

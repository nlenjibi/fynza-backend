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
public class ProductFilterRequest {
    private UUID categoryId;
    private List<UUID> categoryIds;
    private String categoryName;
    private String categorySlug;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minRating;
    private BigDecimal maxRating;
    private String search;
    private String keyword;
    private String name;
    private String sku;
    private String slug;
    private UUID sellerId;
    private String brand;
    private List<String> brands;
    private Boolean inStock;
    private Boolean inStockOnly;
    private Boolean lowStockOnly;
    private Boolean outOfStockOnly;
    private Boolean needsReorderOnly;
    private Boolean hasDiscount;
    private Integer minDiscountPercent;
    private Integer maxDiscountPercent;
    private String status;
    private String inventoryStatus;
    private List<String> inventoryStatuses;
    private Boolean featured;
    private Boolean isNew;
    private Boolean isBestseller;
    private Integer minStock;
    private Integer maxStock;
    private Integer minAvailableQuantity;
    private Long minSales;
    private Boolean popular;
    private Boolean trending;
    private List<String> tags;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private Integer minViews;
    private Integer maxViews;
    private Boolean includeCategory;
    private Boolean includeImages;
}

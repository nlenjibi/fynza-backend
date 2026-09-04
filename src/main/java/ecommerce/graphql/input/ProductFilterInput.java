package ecommerce.graphql.input;

import ecommerce.modules.product.dto.ProductFilterRequest;
import ecommerce.common.enums.InventoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * GraphQL Product Filter Input
 * Matches ProductFilterRequest from REST API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterInput {

    // ==================== Category Filters ====================

    private UUID categoryId;
    private List<UUID> categoryIds;
    private String categoryName;
    private String categorySlug;

    // ==================== Price Filters ====================

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean hasDiscount;
    private Integer minDiscountPercent;
    private Integer maxDiscountPercent;

    // ==================== Search Filters ====================

    private String keyword;
    private String name;
    private String sku;
    private String slug;
    private String brand;
    private List<String> brands;

    // ==================== Marketing Filters ====================

    private Boolean featured;
    private Boolean isNew;
    private Boolean isBestseller;

    // ==================== Inventory Filters ====================

    private InventoryStatus inventoryStatus;
    private List<InventoryStatus> inventoryStatuses;
    private Boolean inStockOnly;
    private Boolean lowStockOnly;
    private Boolean outOfStockOnly;
    private Boolean needsReorderOnly;

    // ==================== Stock Quantity Filters ====================

    private Integer minStock;
    private Integer maxStock;
    private Integer minAvailableQuantity;

    // ==================== Rating Filters ====================

    private BigDecimal minRating;
    private BigDecimal maxRating;

    // ==================== Tag Filters ====================

    private List<String> tags;

    // ==================== Date Filters ====================

    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    // ==================== View/Sales Filters ====================

    private Integer minViews;
    private Integer maxViews;
    private Long minSales;
    private Boolean popular;
    private Boolean trending;

    // ==================== Eager Loading Options ====================

    @Builder.Default
    private Boolean includeCategory = true;

    @Builder.Default
    private Boolean includeImages = false;

    /**
     * Check if any filter is applied
     */
    public boolean hasFilters() {
        return categoryId != null ||
                (categoryIds != null && !categoryIds.isEmpty()) ||
                categoryName != null || categorySlug != null ||
                minPrice != null || maxPrice != null ||
                keyword != null || name != null || sku != null || slug != null ||
                featured != null || isNew != null || isBestseller != null ||
                inventoryStatus != null ||
                (inventoryStatuses != null && !inventoryStatuses.isEmpty()) ||
                inStockOnly != null || lowStockOnly != null ||
                outOfStockOnly != null || needsReorderOnly != null ||
                hasDiscount != null || minDiscountPercent != null ||
                maxDiscountPercent != null ||
                minRating != null || maxRating != null ||
                minStock != null || maxStock != null ||
                minAvailableQuantity != null ||
                (tags != null && !tags.isEmpty()) ||
                createdAfter != null || createdBefore != null ||
                minViews != null || maxViews != null ||
                minSales != null || popular != null || trending != null;
    }

    /**
     * Convert to ProductFilterRequest for service layer
     */
    public ProductFilterRequest toFilterRequest() {
        return ProductFilterRequest.builder()
                .categoryId(categoryId)
                .categoryIds(categoryIds)
                .categoryName(categoryName)
                .categorySlug(categorySlug)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .hasDiscount(hasDiscount)
                .minDiscountPercent(minDiscountPercent)
                .maxDiscountPercent(maxDiscountPercent)
                .keyword(keyword)
                .name(name)
                .sku(sku)
                .slug(slug)
                .featured(featured)
                .isNew(isNew)
                .isBestseller(isBestseller)
                .inventoryStatus(inventoryStatus != null ? inventoryStatus.name() : null)
                .inventoryStatuses(inventoryStatuses != null ? inventoryStatuses.stream()
                        .map(ecommerce.common.enums.InventoryStatus::name).collect(java.util.stream.Collectors.toList()) : null)
                .inStockOnly(inStockOnly)
                .lowStockOnly(lowStockOnly)
                .outOfStockOnly(outOfStockOnly)
                .needsReorderOnly(needsReorderOnly)
                .minStock(minStock)
                .maxStock(maxStock)
                .minAvailableQuantity(minAvailableQuantity)
                .minRating(minRating)
                .maxRating(maxRating)
                .tags(tags)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .minViews(minViews)
                .maxViews(maxViews)
                .minSales(minSales)
                .popular(popular)
                .trending(trending)
                .includeCategory(includeCategory)
                .includeImages(includeImages)
                .build();
    }
}

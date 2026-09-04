package ecommerce.graphql.resolver.product;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.ProductDto;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.ProductCreateInput;
import ecommerce.graphql.input.ProductFilterInput;
import ecommerce.graphql.input.ProductUpdateInput;
import ecommerce.graphql.input.SearchInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.product.dto.*;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.product.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductResolver {

    private final ProductService productService;
    private final SearchService searchService;

    // =========================================================================
    // PUBLIC PRODUCT QUERIES
    // =========================================================================

    @QueryMapping
    public ProductResponse product(@Argument UUID id) {
        log.info("GQL product(id={})", id);
        return productService.findById(id);
    }

    @QueryMapping
    public ProductDto products(@Argument PageInput pagination,
                                @Argument ProductFilterInput filter) {
        log.info("GQL products");
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filterRequest = filter != null ? filter.toFilterRequest() : null;
        Page<ProductResponse> page = productService.findAll(filterRequest, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public ProductDto productsByCategory(@Argument UUID categoryId,
                                          @Argument PageInput pagination) {
        log.info("GQL productsByCategory(categoryId={})", categoryId);
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .categoryId(categoryId)
                .build();
        Page<ProductResponse> page = productService.findAll(filter, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public ProductDto productsByCategoryName(@Argument String categoryName,
                                              @Argument PageInput pagination) {
        log.info("GQL productsByCategoryName(categoryName={})", categoryName);
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .categoryName(categoryName)
                .build();
        Page<ProductResponse> page = productService.findAll(filter, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public ProductDto productsByPriceRange(@Argument BigDecimal minPrice,
                                            @Argument BigDecimal maxPrice,
                                            @Argument PageInput pagination) {
        log.info("GQL productsByPriceRange(min={}, max={})", minPrice, maxPrice);
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
        Page<ProductResponse> page = productService.findAll(filter, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public ProductDto searchProducts(@Argument String keyword,
                                      @Argument PageInput pagination) {
        log.info("GQL searchProducts(keyword={})", keyword);
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .keyword(keyword)
                .build();
        Page<ProductResponse> page = productService.findAll(filter, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public List<ProductResponse> popularProducts(@Argument int limit,
                                                  @Argument UUID categoryId) {
        log.info("GQL popularProducts(limit={}, categoryId={})", limit, categoryId);
        return searchService.getPopularProducts(limit, categoryId);
    }

    // =========================================================================
    // SEARCH QUERIES
    // =========================================================================

    @QueryMapping
    public SearchResponse search(@Argument SearchInput input) {
        log.info("GQL search(q={})", input != null ? input.getQ() : null);
        SearchRequest request = mapToSearchRequest(input);
        return searchService.search(request);
    }

    @QueryMapping
    public List<String> searchSuggestions(@Argument String query,
                                           @Argument int limit) {
        log.info("GQL searchSuggestions(query={})", query);
        return searchService.getSuggestions(query, limit);
    }

    @QueryMapping
    public List<String> trendingSearches(@Argument int limit) {
        log.info("GQL trendingSearches");
        return searchService.getTrending(limit, "week");
    }

    // =========================================================================
    // ADMIN/SELLER PRODUCT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto productsBySeller(@Argument UUID sellerId,
                                        @Argument PageInput pagination) {
        log.info("GQL productsBySeller(sellerId={})", sellerId);
        Pageable pageable = toPageable(pagination);
        Page<ProductResponse> page = productService.findBySellerId(sellerId, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto productsByInventoryStatus(@Argument String status,
                                                  @Argument PageInput pagination) {
        log.info("GQL productsByInventoryStatus(status={})", status);
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .inventoryStatus(status)
                .build();
        Page<ProductResponse> page = productService.findAll(filter, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto productsNeedingReorder(@Argument PageInput pagination) {
        log.info("GQL productsNeedingReorder");
        Pageable pageable = toPageable(pagination);
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .needsReorderOnly(true)
                .build();
        Page<ProductResponse> page = productService.findAll(filter, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductResponse> lowStockProducts() {
        log.info("GQL lowStockProducts");
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .lowStockOnly(true)
                .build();
        return productService.findAll(filter, PageRequest.of(0, 50)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductResponse> outOfStockProducts() {
        log.info("GQL outOfStockProducts");
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .outOfStockOnly(true)
                .build();
        return productService.findAll(filter, PageRequest.of(0, 50)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminProductStatsResponse adminProductStats() {
        log.info("GQL adminProductStats");
        return productService.getAdminProductStats();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerProductStatsResponse sellerProductStats(@ContextValue UUID sellerId) {
        log.info("GQL sellerProductStats(seller={})", sellerId);
        return productService.getSellerProductStats(sellerId);
    }

    // =========================================================================
    // PRODUCT CRUD MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse createProduct(@Argument ProductCreateInput input,
                                          @ContextValue UUID userId) {
        log.info("GQL createProduct(name={}, user={})", input.getName(), userId);
        CreateProductRequest request = CreateProductRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .brand(input.getBrand())
                .sku(input.getSku())
                .price(input.getPrice())
                .originalPrice(input.getOriginalPrice())
                .categoryId(input.getCategoryId())
                .stock(input.getStock())
                .images(input.getImages())
                .build();
        return productService.create(request, userId);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse updateProduct(@Argument UUID id,
                                          @Argument ProductUpdateInput input) {
        log.info("GQL updateProduct(id={})", id);
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .brand(input.getBrand())
                .sku(input.getSku())
                .price(input.getPrice())
                .originalPrice(input.getOriginalPrice())
                .categoryId(input.getCategoryId())
                .stock(input.getStock())
                .images(input.getImages())
                .build();
        return productService.update(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public boolean deleteProduct(@Argument UUID id) {
        log.info("GQL deleteProduct(id={})", id);
        productService.delete(id);
        return true;
    }

    // =========================================================================
    // STOCK MANAGEMENT MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse addStock(@Argument UUID id, @Argument int quantity) {
        log.info("GQL addStock(id={}, quantity={})", id, quantity);
        return productService.addStock(id, quantity);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse reduceStock(@Argument UUID id, @Argument int quantity) {
        log.info("GQL reduceStock(id={}, quantity={})", id, quantity);
        return productService.reduceStock(id, quantity);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse reserveStock(@Argument UUID id, @Argument int quantity) {
        log.info("GQL reserveStock(id={}, quantity={})", id, quantity);
        return productService.reserveStock(id, quantity);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse releaseReservedStock(@Argument UUID id, @Argument int quantity) {
        log.info("GQL releaseReservedStock(id={}, quantity={})", id, quantity);
        return productService.releaseReservedStock(id, quantity);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ProductResponse restoreStock(@Argument UUID id, @Argument int quantity) {
        log.info("GQL restoreStock(id={}, quantity={})", id, quantity);
        return productService.restoreStock(id, quantity);
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse approveProduct(@Argument UUID id) {
        log.info("GQL approveProduct(id={})", id);
        return productService.approveProduct(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse rejectProduct(@Argument UUID id, @Argument String reason) {
        log.info("GQL rejectProduct(id={}, reason={})", id, reason);
        return productService.rejectProduct(id, reason);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public boolean bulkUpdateFeatured(@Argument List<UUID> productIds,
                                       @Argument boolean featured) {
        log.info("GQL bulkUpdateFeatured(productIds={}, featured={})", productIds, featured);
        for (UUID id : productIds) {
            try {
                productService.updateProductStatus(id, featured ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
            } catch (Exception e) {
                log.warn("Failed to update product {}: {}", id, e.getMessage());
            }
        }
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean bulkDelete(@Argument List<UUID> productIds) {
        log.info("GQL bulkDelete(productIds={})", productIds);
        for (UUID id : productIds) {
            try {
                productService.delete(id);
            } catch (Exception e) {
                log.warn("Failed to delete product {}: {}", id, e.getMessage());
            }
        }
        return true;
    }

    // =========================================================================
    // ANALYTICS MUTATIONS
    // =========================================================================

    @MutationMapping
    public boolean incrementViewCount(@Argument UUID id) {
        log.info("GQL incrementViewCount(id={})", id);
        return productService.incrementViewCount(id);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ProductResponse updateRating(@Argument UUID id, @Argument float rating) {
        log.info("GQL updateRating(id={}, rating={})", id, rating);
        return productService.updateRating(id, rating);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    private SearchRequest mapToSearchRequest(SearchInput input) {
        if (input == null) {
            return SearchRequest.builder().build();
        }
        return SearchRequest.builder()
                .q(input.getQ())
                .categoryId(input.getCategoryId() != null ? UUID.fromString(input.getCategoryId()) : null)
                .brandId(input.getBrandId() != null ? UUID.fromString(input.getBrandId()) : null)
                .minPrice(input.getMinPrice())
                .maxPrice(input.getMaxPrice())
                .minRating(input.getMinRating() != null ? BigDecimal.valueOf(input.getMinRating()) : null)
                .maxRating(input.getMaxRating() != null ? BigDecimal.valueOf(input.getMaxRating()) : null)
                .inStock(input.getInStock())
                .expressDelivery(input.getExpressDelivery())
                .discountMin(input.getDiscountMin() != null ? BigDecimal.valueOf(input.getDiscountMin()) : null)
                .discountMax(input.getDiscountMax() != null ? BigDecimal.valueOf(input.getDiscountMax()) : null)
                .sortBy(input.getSortBy() != null ? input.getSortBy() : "popularity")
                .page(input.getPage() != null ? input.getPage() : 0)
                .limit(input.getLimit() != null ? input.getLimit() : 20)
                .build();
    }
}

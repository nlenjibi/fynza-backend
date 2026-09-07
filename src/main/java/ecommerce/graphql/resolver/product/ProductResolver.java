package ecommerce.graphql.resolver.product;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.ProductDto;
import ecommerce.common.enums.ProductStatus;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.ProductFilterInput;
import ecommerce.graphql.input.SearchInput;
import ecommerce.graphql.input.SellerProductFilterInput;
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
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ProductDto products(@Argument PageInput pagination, @Argument ProductFilterInput filter) {
        log.info("GQL products");
        ProductFilterRequest filterRequest = filter != null ? filter.toFilterRequest() : null;
        Page<ProductResponse> page = productService.findAll(filterRequest, toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    public ProductDto productsByCategory(@Argument UUID categoryId, @Argument PageInput pagination) {
        log.info("GQL productsByCategory(categoryId={})", categoryId);
        Page<ProductResponse> page = productService.findAll(
                ProductFilterRequest.builder().categoryId(categoryId).build(), toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    public ProductDto productsByCategoryName(@Argument String categoryName, @Argument PageInput pagination) {
        log.info("GQL productsByCategoryName(categoryName={})", categoryName);
        Page<ProductResponse> page = productService.findAll(
                ProductFilterRequest.builder().categoryName(categoryName).build(), toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    public ProductDto productsByPriceRange(@Argument BigDecimal minPrice,
                                           @Argument BigDecimal maxPrice,
                                           @Argument PageInput pagination) {
        log.info("GQL productsByPriceRange(min={}, max={})", minPrice, maxPrice);
        Page<ProductResponse> page = productService.findAll(
                ProductFilterRequest.builder().minPrice(minPrice).maxPrice(maxPrice).build(), toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    public ProductDto searchProducts(@Argument String keyword, @Argument PageInput pagination) {
        log.info("GQL searchProducts(keyword={})", keyword);
        Page<ProductResponse> page = productService.findAll(
                ProductFilterRequest.builder().keyword(keyword).build(), toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    public List<ProductResponse> popularProducts(@Argument int limit, @Argument UUID categoryId) {
        log.info("GQL popularProducts(limit={}, categoryId={})", limit, categoryId);
        return searchService.getPopularProducts(limit, categoryId);
    }

    // =========================================================================
    // SEARCH QUERIES
    // =========================================================================

    @QueryMapping
    public SearchResponse search(@Argument SearchInput input) {
        log.info("GQL search(q={})", input != null ? input.getQ() : null);
        return searchService.search(mapToSearchRequest(input));
    }

    @QueryMapping
    public List<String> searchSuggestions(@Argument String query, @Argument int limit) {
        log.info("GQL searchSuggestions(query={})", query);
        return searchService.getSuggestions(query, limit);
    }

    @QueryMapping
    public List<String> trendingSearches(@Argument int limit) {
        log.info("GQL trendingSearches");
        return searchService.getTrending(limit, "week");
    }

    // =========================================================================
    // SELLER PRODUCT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductDto sellerProducts(@Argument PageInput pagination,
                                     @Argument SellerProductFilterInput filter,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerProducts(seller={})", principal.getId());
        Pageable pageable = toPageable(pagination);
        ProductStatus status = null;
        UUID categoryId = null;
        String search = null;
        if (filter != null) {
            if (filter.getStatus() != null) status = ProductStatus.valueOf(filter.getStatus().toUpperCase());
            categoryId = filter.getCategoryId();
            search = filter.getSearch();
        }
        Page<ProductResponse> page = productService.findBySellerId(principal.getId(), status, categoryId, search, pageable);
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    // =========================================================================
    // ADMIN PRODUCT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto productsBySeller(@Argument UUID sellerId, @Argument PageInput pagination) {
        log.info("GQL productsBySeller(sellerId={})", sellerId);
        Page<ProductResponse> page = productService.findBySellerId(sellerId, toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto productsByInventoryStatus(@Argument String status, @Argument PageInput pagination) {
        log.info("GQL productsByInventoryStatus(status={})", status);
        Page<ProductResponse> page = productService.findAll(
                ProductFilterRequest.builder().inventoryStatus(status).build(), toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductDto productsNeedingReorder(@Argument PageInput pagination) {
        log.info("GQL productsNeedingReorder");
        Page<ProductResponse> page = productService.findAll(
                ProductFilterRequest.builder().needsReorderOnly(true).build(), toPageable(pagination));
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductResponse> lowStockProducts() {
        log.info("GQL lowStockProducts");
        return productService.findAll(ProductFilterRequest.builder().lowStockOnly(true).build(), PageRequest.of(0, 50)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<ProductResponse> outOfStockProducts() {
        log.info("GQL outOfStockProducts");
        return productService.findAll(ProductFilterRequest.builder().outOfStockOnly(true).build(), PageRequest.of(0, 50)).getContent();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminProductStatsResponse adminProductStats() {
        log.info("GQL adminProductStats");
        return productService.getAdminProductStats();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerProductStatsResponse sellerProductStats(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerProductStats(seller={})", principal.getId());
        return productService.getSellerProductStats(principal.getId());
    }

    // Product mutations are REST-only per PRD §86.
    // Use: POST /v1/products, PUT /v1/products/{id}, DELETE /v1/products/{id}
    //      POST /v1/products/{id}/stock/*, PATCH /v1/products/{id}/moderation

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
        if (input == null) return SearchRequest.builder().build();
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

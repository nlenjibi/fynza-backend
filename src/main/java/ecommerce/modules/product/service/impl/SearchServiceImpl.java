package ecommerce.modules.product.service.impl;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.product.async.SearchAsyncService;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.dto.SearchRequest;
import ecommerce.modules.product.dto.SearchResponse;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.service.SearchService;
import ecommerce.modules.product.spec.ProductSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityService securityService;
    private final SearchAsyncService searchAsyncService;

    @Override
    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        Specification<Product> spec = buildSpec(request);

        Sort sort = buildSort(request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getLimit(), sort);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<ProductResponse> results = productPage.getContent().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());

        SearchResponse.Filters filters = getCachedFilters();

        searchAsyncService.recordSearchQuery(request);

        return SearchResponse.builder()
                .results(results)
                .pagination(buildPagination(productPage, request))
                .filters(filters)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        return productRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, limit))
                .stream()
                .map(Product::getName)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTrending(int limit, String timeRange) {
        return productRepository.findTopByViewCount(ProductStatus.ACTIVE, PageRequest.of(0, limit)).stream()
                .map(Product::getName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPopularProducts(int limit, UUID categoryPublicId) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Product> products;

        if (categoryPublicId != null) {
            Specification<Product> spec = Specification
                    .where(ProductSpec.hasCategoryPublicId(categoryPublicId))
                    .and(ProductSpec.hasStatus(ProductStatus.ACTIVE));
            products = productRepository.findAll(spec, pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }

        return products.getContent().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    private Specification<Product> buildSpec(SearchRequest request) {
        boolean isAdmin = securityService.isAdmin();
        ProductStatus defaultStatus = isAdmin ? null : ProductStatus.ACTIVE;

        return Specification
                .where(ProductSpec.hasStatus(defaultStatus))
                .and(ProductSpec.nameOrDescriptionContains(request.getQ()))
                .and(ProductSpec.hasCategoryPublicId(request.getCategoryId()))
                .and(ProductSpec.hasSellerPublicId(request.getBrandId()))
                .and(ProductSpec.priceBetween(request.getMinPrice(), request.getMaxPrice()))
                .and(ProductSpec.ratingBetween(request.getMinRating(), request.getMaxRating()))
                .and(Boolean.TRUE.equals(request.getInStock()) ? ProductSpec.isInStock() : null)
                .and(ProductSpec.discountBetween(request.getDiscountMin(), request.getDiscountMax()));
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by("viewCount").descending();
        return switch (sortBy) {
            case "price-low"  -> Sort.by("price").ascending();
            case "price-high" -> Sort.by("price").descending();
            case "rating"     -> Sort.by("rating").descending();
            case "newest"     -> Sort.by("createdAt").descending();
            default           -> Sort.by("viewCount").descending();
        };
    }

    private SearchResponse.PageInfo buildPagination(Page<Product> page, SearchRequest request) {
        return SearchResponse.PageInfo.builder()
                .page(request.getPage())
                .limit(request.getLimit())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Cacheable(value = "searchFilters", key = "'all'")
    public SearchResponse.Filters getCachedFilters() {
        log.debug("Building search filters (cached)");
        List<Category> categories = categoryRepository.findAll();

        List<SearchResponse.CategoryFilter> categoryFilters = categories.stream()
                .map(cat -> SearchResponse.CategoryFilter.builder()
                        .id(cat.getPublicId())
                        .name(cat.getName())
                        .productCount(0L)
                        .build())
                .collect(Collectors.toList());

        return SearchResponse.Filters.builder()
                .categories(categoryFilters)
                .brands(List.of())
                .priceRange(SearchResponse.PriceRange.builder()
                        .min(BigDecimal.ZERO)
                        .max(BigDecimal.valueOf(10000))
                        .build())
                .ratingRange(SearchResponse.RatingRange.builder()
                        .min(BigDecimal.ZERO)
                        .max(BigDecimal.valueOf(5))
                        .build())
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getPublicId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discount(product.getDiscount())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .inStock(product.isInStock())
                .stockCount(product.getStock())
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}

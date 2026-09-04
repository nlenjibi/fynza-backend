package ecommerce.modules.product.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.dto.SearchRequest;
import ecommerce.modules.product.dto.SearchResponse;
import ecommerce.modules.product.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("api/v1/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Search", description = "APIs for product search with filtering, sorting, and pagination")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search products", description = "Main search endpoint with all filters, sorting, and pagination")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @Parameter(description = "Search query")
            @RequestParam(required = false) String q,
            @Parameter(description = "Category ID")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Brand/Seller ID")
            @RequestParam(required = false) UUID brandId,
            @Parameter(description = "Minimum price")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum rating (0-5)")
            @RequestParam(required = false) BigDecimal minRating,
            @Parameter(description = "Maximum rating (0-5)")
            @RequestParam(required = false) BigDecimal maxRating,
            @Parameter(description = "Filter by in-stock items")
            @RequestParam(required = false) Boolean inStock,
            @Parameter(description = "Filter by express delivery")
            @RequestParam(required = false) Boolean expressDelivery,
            @Parameter(description = "Minimum discount percentage")
            @RequestParam(required = false) BigDecimal discountMin,
            @Parameter(description = "Maximum discount percentage")
            @RequestParam(required = false) BigDecimal discountMax,
            @Parameter(description = "Sort by: popularity, price-low, price-high, rating, newest")
            @RequestParam(defaultValue = "popularity") String sortBy,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Results per page (max 100)")
            @RequestParam(defaultValue = "20") int limit) {

        SearchRequest request = SearchRequest.builder()
                .q(q)
                .categoryId(categoryId)
                .brandId(brandId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .maxRating(maxRating)
                .inStock(inStock)
                .expressDelivery(expressDelivery)
                .discountMin(discountMin)
                .discountMax(discountMax)
                .sortBy(sortBy)
                .page(page)
                .limit(limit)
                .build();

        log.info("Search request: q={}, categoryId={}, brandId={}, minPrice={}, maxPrice={}, sortBy={}, page={}, limit={}",
                q, categoryId, brandId, minPrice, maxPrice, sortBy, page, limit);

        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Autocomplete suggestions based on query")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(description = "Maximum number of suggestions")
            @RequestParam(defaultValue = "10") int limit) {

        List<String> suggestions = searchService.getSuggestions(query, limit);
        return ResponseEntity.ok(ApiResponse.success("Suggestions retrieved successfully", suggestions));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending searches", description = "Get trending search terms")
    public ResponseEntity<ApiResponse<List<String>>> getTrending(
            @Parameter(description = "Maximum number of trending terms")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Time range: day, week, month")
            @RequestParam(defaultValue = "week") String timeRange) {

        List<String> trending = searchService.getTrending(limit, timeRange);
        return ResponseEntity.ok(ApiResponse.success("Trending searches retrieved successfully", trending));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular products", description = "Get popular products, optionally filtered by category")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPopularProducts(
            @Parameter(description = "Maximum number of products")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Category ID to filter by")
            @RequestParam(required = false) UUID categoryId) {

        List<ProductResponse> products = searchService.getPopularProducts(limit, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Popular products retrieved successfully", products));
    }
}

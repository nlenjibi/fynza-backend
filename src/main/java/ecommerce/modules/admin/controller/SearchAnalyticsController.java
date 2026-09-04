package ecommerce.modules.admin.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.admin.entity.SearchAnalytics;
import ecommerce.modules.admin.service.SearchAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for search analytics endpoints.
 */
@RestController
@RequestMapping("/v1/admin/search-analytics")
@RequiredArgsConstructor
@Tag(name = "Search Analytics", description = "Search analytics endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    @GetMapping("/top-searches")
    @Operation(summary = "Get top searches", description = "Get most popular search queries")
    public ResponseEntity<ApiResponse<PaginatedResponse<SearchAnalytics>>> getTopSearches(
            @RequestParam(defaultValue = "30") int days,
            Pageable pageable) {
        Page<SearchAnalytics> searches = searchAnalyticsService.getTopSearches(days, pageable);
        return ResponseEntity.ok(ApiResponse.success("Top searches retrieved successfully", PaginatedResponse.from(searches)));
    }

    @GetMapping("/most-popular")
    @Operation(summary = "Get most popular searches", description = "Get most popular search queries by count")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMostPopularSearches(
            @RequestParam(defaultValue = "20") int limit) {
        List<Object[]> searches = searchAnalyticsService.getMostPopularSearches(limit);
        return ResponseEntity.ok(ApiResponse.success("Most popular searches retrieved successfully", searches));
    }

    @GetMapping("/most-clicked")
    @Operation(summary = "Get most clicked searches", description = "Get search queries with most clicks")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMostClickedSearches(
            @RequestParam(defaultValue = "20") int limit) {
        List<Object[]> searches = searchAnalyticsService.getMostClickedSearches(limit);
        return ResponseEntity.ok(ApiResponse.success("Most clicked searches retrieved successfully", searches));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get search trends", description = "Get search trends over time")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSearchTrends(
            @RequestParam(defaultValue = "30") int days) {
        List<Object[]> trends = searchAnalyticsService.getSearchTrends(days);
        return ResponseEntity.ok(ApiResponse.success("Search trends retrieved successfully", trends));
    }

    @GetMapping("/type-distribution")
    @Operation(summary = "Get search type distribution", description = "Get distribution of search types")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSearchTypeDistribution(
            @RequestParam(defaultValue = "30") int days) {
        List<Object[]> distribution = searchAnalyticsService.getSearchTypeDistribution(days);
        return ResponseEntity.ok(ApiResponse.success("Search type distribution retrieved successfully", distribution));
    }

    @GetMapping("/zero-result-rate")
    @Operation(summary = "Get zero result rate", description = "Get percentage of searches with zero results")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getZeroResultRate(
            @RequestParam(defaultValue = "30") int days) {
        double rate = searchAnalyticsService.getZeroResultRate(days);
        return ResponseEntity.ok(ApiResponse.success("Zero result rate retrieved successfully", Map.of("rate", rate)));
    }
}

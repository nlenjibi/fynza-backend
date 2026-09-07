package ecommerce.modules.search.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.search.entity.SearchAnalytics;
import ecommerce.modules.search.service.SearchAnalyticsService;
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

@RestController
@RequestMapping("/v1/search/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Search Analytics", description = "Search query analytics — Admin only")
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    @Operation(summary = "Get top searches")
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<PaginatedResponse<SearchAnalytics>>> getTopSearches(
            @RequestParam(defaultValue = "30") int days,
            Pageable pageable) {
        Page<SearchAnalytics> searches = searchAnalyticsService.getTopSearches(days, pageable);
        return ResponseEntity.ok(ApiResponse.success("Top searches retrieved.", PaginatedResponse.from(searches)));
    }

    @Operation(summary = "Get most popular searches")
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMostPopularSearches(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Most popular searches retrieved.",
                searchAnalyticsService.getMostPopularSearches(limit)));
    }

    @Operation(summary = "Get most clicked searches")
    @GetMapping("/clicked")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMostClickedSearches(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Most clicked searches retrieved.",
                searchAnalyticsService.getMostClickedSearches(limit)));
    }

    @Operation(summary = "Get search trends over time")
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSearchTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success("Search trends retrieved.",
                searchAnalyticsService.getSearchTrends(days)));
    }

    @Operation(summary = "Get search type distribution")
    @GetMapping("/type-distribution")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSearchTypeDistribution(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success("Search type distribution retrieved.",
                searchAnalyticsService.getSearchTypeDistribution(days)));
    }

    @Operation(summary = "Get zero result rate")
    @GetMapping("/zero-result-rate")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getZeroResultRate(
            @RequestParam(defaultValue = "30") int days) {
        double rate = searchAnalyticsService.getZeroResultRate(days);
        return ResponseEntity.ok(ApiResponse.success("Zero result rate retrieved.", Map.of("rate", rate)));
    }
}

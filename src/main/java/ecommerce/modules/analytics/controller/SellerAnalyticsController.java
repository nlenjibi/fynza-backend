package ecommerce.modules.analytics.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.analytics.dto.SellerAnalyticsDto;
import ecommerce.modules.analytics.dto.SellerAnalyticsResponse;
import ecommerce.modules.analytics.dto.SellerDashboardResponse;
import ecommerce.modules.analytics.service.SellerAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Analytics", description = "Seller dashboard and analytics")
public class SellerAnalyticsController {

    private final SellerAnalyticsService sellerAnalyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get seller dashboard")
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully",
                sellerAnalyticsService.getDashboard(principal.getId())));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get seller analytics")
    public ResponseEntity<ApiResponse<SellerAnalyticsDto>> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully",
                sellerAnalyticsService.getSellerAnalytics(principal.getId())));
    }

    @GetMapping("/analytics/sales")
    @Operation(summary = "Get seller sales analytics")
    public ResponseEntity<ApiResponse<SellerAnalyticsResponse>> getSalesAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success("Sales analytics retrieved successfully",
                sellerAnalyticsService.getSalesAnalytics(principal.getId(), days)));
    }
}

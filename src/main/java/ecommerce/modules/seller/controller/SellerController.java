package ecommerce.modules.seller.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.order.dto.SellerOrderStatsResponse;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.dto.CreateProductRequest;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.dto.SellerProductStatsResponse;
import ecommerce.modules.product.dto.UpdateProductRequest;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewStatsResponse;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.seller.dto.*;
import ecommerce.modules.seller.service.SellerService;
import ecommerce.modules.tag.dto.TagResponse;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller Management", description = "Seller management endpoints")
public class SellerController {

    private final SellerService sellerService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller dashboard", description = "Get dashboard statistics for the authenticated seller")
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerDashboardResponse dashboard = sellerService.getDashboard(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller products", description = "Get all products for the authenticated seller with filters")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        UUID sellerId = UUID.fromString(principal.getId().toString());
        Page<ProductResponse> products = productService.findBySellerId(sellerId, status, categoryId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    @GetMapping("/products/stats")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller product stats", description = "Get product statistics for the authenticated seller")
    public ResponseEntity<ApiResponse<SellerProductStatsResponse>> getProductStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        SellerProductStatsResponse stats = productService.getSellerProductStats(sellerId);
        return ResponseEntity.ok(ApiResponse.success("Product stats retrieved successfully", stats));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create product", description = "Create a new product for the authenticated seller")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            UriComponentsBuilder uriBuilder) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        ProductResponse response = productService.create(request, sellerId);
        var uri = uriBuilder.path("/api/v1/sellers/products/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update product", description = "Update an existing product for the authenticated seller")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existingProduct = productService.findById(id);
        
        boolean isOwner = existingProduct.getSeller() != null && 
                existingProduct.getSeller().getId().toString().equals(principal.getId().toString());
        
        if (!isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to update this product"));
        }
        
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete product", description = "Delete a product for the authenticated seller")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existingProduct = productService.findById(id);
        
        boolean isOwner = existingProduct.getSeller() != null && 
                existingProduct.getSeller().getId().toString().equals(principal.getId().toString());
        
        if (!isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to delete this product"));
        }
        
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller orders", description = "Get all orders for the authenticated seller with filters")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        UUID sellerId = UUID.fromString(principal.getId().toString());
        
        java.time.LocalDateTime dateFrom = null;
        java.time.LocalDateTime dateTo = null;
        
        if (dateFilter != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            switch (dateFilter.toLowerCase()) {
                case "today" -> dateFrom = now.toLocalDate().atStartOfDay();
                case "week" -> dateFrom = now.minusWeeks(1);
                case "month" -> dateFrom = now.minusMonths(1);
                case "year" -> dateFrom = now.minusYears(1);
            }
        }
        
        Page<OrderResponse> orders = orderService.getSellerOrders(sellerId, status, dateFrom, dateTo, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/orders/stats")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller order stats", description = "Get order statistics for the authenticated seller")
    public ResponseEntity<ApiResponse<SellerOrderStatsResponse>> getOrderStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        SellerOrderStatsResponse stats = orderService.getSellerOrderStats(sellerId);
        return ResponseEntity.ok(ApiResponse.success("Order stats retrieved successfully", stats));
    }

    @GetMapping("/orders/export")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Export seller orders", description = "Export seller orders to CSV")
    public ResponseEntity<String> exportOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String search) {
        
        UUID sellerId = UUID.fromString(principal.getId().toString());
        
        java.time.LocalDateTime dateFrom = null;
        java.time.LocalDateTime dateTo = null;
        
        if (dateFilter != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            switch (dateFilter.toLowerCase()) {
                case "today" -> dateFrom = now.toLocalDate().atStartOfDay();
                case "week" -> dateFrom = now.minusWeeks(1);
                case "month" -> dateFrom = now.minusMonths(1);
                case "year" -> dateFrom = now.minusYears(1);
            }
        }
        
        String csv = orderService.exportSellerOrdersToCSV(sellerId, status, dateFrom, dateTo, search);
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=orders.csv")
                .body(csv);
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update order status", description = "Update order status for the authenticated seller")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        OrderResponse order = sellerService.updateOrderStatus(id, request, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }

    @GetMapping("/analytics/sales")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get sales analytics", description = "Get sales analytics for the authenticated seller")
    public ResponseEntity<ApiResponse<SellerAnalyticsResponse>> getSalesAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "30") int days) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        SellerAnalyticsResponse analytics = sellerService.getSalesAnalytics(sellerId, days);
        return ResponseEntity.ok(ApiResponse.success("Sales analytics retrieved successfully", analytics));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller analytics", description = "Get comprehensive analytics for seller dashboard")
    public ResponseEntity<ApiResponse<SellerAnalyticsDto>> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        SellerAnalyticsDto analytics = sellerService.getSellerAnalytics(sellerId);
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }

    @GetMapping("/store")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get store info", description = "Get store information for the authenticated seller")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        StoreResponse store = sellerService.getStore(sellerId);
        return ResponseEntity.ok(ApiResponse.success("Store retrieved successfully", store));
    }

    @PutMapping("/store")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update store info", description = "Update store information for the authenticated seller")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Valid @RequestBody UpdateStoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        StoreResponse store = sellerService.updateStore(sellerId, request);
        return ResponseEntity.ok(ApiResponse.success("Store updated successfully", store));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller reviews", description = "Get reviews for all products of the authenticated seller")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getSellerReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        UUID sellerId = UUID.fromString(principal.getId().toString());
        Page<ReviewResponse> reviews = sellerService.getSellerReviews(sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", reviews));
    }

    @GetMapping("/reviews/stats")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller review statistics", description = "Get review statistics for all products of the seller")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getSellerReviewStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success(reviewService.getSellerReviewStats(sellerId)));
    }

    @PostMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Reply to review", description = "Seller replies to a review on their product")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyToReview(
            @PathVariable UUID reviewId,
            @RequestBody java.util.Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        String reply = request.get("reply");
        ReviewResponse response = reviewService.sellerReply(reviewId, sellerId, reply);
        return ResponseEntity.ok(ApiResponse.success("Reply added successfully", response));
    }

    @GetMapping("/tags")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get available tags", description = "Get all available tags for products")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags() {
        return ResponseEntity.ok(ApiResponse.success(
                "Tags retrieved successfully",
                sellerService.getTags()));
    }

    @PostMapping("/products/{id}/tags")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Assign tags to product", description = "Assign tags to a product")
    public ResponseEntity<ApiResponse<Void>> assignTags(
            @PathVariable UUID id,
            @RequestBody List<String> tags,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        sellerService.assignTagsToProduct(id, tags, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Tags assigned successfully", null));
    }

    @GetMapping("/settings/payment")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get payment settings", description = "Get seller payment settings")
    public ResponseEntity<ApiResponse<SellerPaymentSettingsResponse>> getPaymentSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Payment settings retrieved", 
                sellerService.getPaymentSettings(sellerId)));
    }

    @PutMapping("/settings/payment")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update payment settings", description = "Update seller payment settings")
    public ResponseEntity<ApiResponse<SellerPaymentSettingsResponse>> updatePaymentSettings(
            @Valid @RequestBody SellerPaymentSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Payment settings updated", 
                sellerService.updatePaymentSettings(sellerId, request)));
    }

    @GetMapping("/settings/shipping")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get shipping settings", description = "Get seller shipping settings")
    public ResponseEntity<ApiResponse<SellerShippingSettingsResponse>> getShippingSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Shipping settings retrieved", 
                sellerService.getShippingSettings(sellerId)));
    }

    @PutMapping("/settings/shipping")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update shipping settings", description = "Update seller shipping settings")
    public ResponseEntity<ApiResponse<SellerShippingSettingsResponse>> updateShippingSettings(
            @Valid @RequestBody SellerShippingSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Shipping settings updated", 
                sellerService.updateShippingSettings(sellerId, request)));
    }

    @GetMapping("/settings/shipping/zones")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get shipping zones", description = "Get seller shipping zones")
    public ResponseEntity<ApiResponse<List<ShippingZoneResponse>>> getShippingZones(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Shipping zones retrieved", 
                sellerService.getShippingZones(sellerId)));
    }

    @PostMapping("/settings/shipping/zones")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create shipping zone", description = "Create a new shipping zone")
    public ResponseEntity<ApiResponse<ShippingZoneResponse>> createShippingZone(
            @Valid @RequestBody ShippingZoneRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping zone created", 
                        sellerService.createShippingZone(sellerId, request)));
    }

    @PutMapping("/settings/shipping/zones/{zoneId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update shipping zone", description = "Update an existing shipping zone")
    public ResponseEntity<ApiResponse<ShippingZoneResponse>> updateShippingZone(
            @PathVariable UUID zoneId,
            @Valid @RequestBody ShippingZoneRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Shipping zone updated", 
                sellerService.updateShippingZone(sellerId, zoneId, request)));
    }

    @DeleteMapping("/settings/shipping/zones/{zoneId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete shipping zone", description = "Delete a shipping zone")
    public ResponseEntity<ApiResponse<Void>> deleteShippingZone(
            @PathVariable UUID zoneId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        sellerService.deleteShippingZone(sellerId, zoneId);
        return ResponseEntity.ok(ApiResponse.success("Shipping zone deleted", null));
    }

    @GetMapping("/settings/notifications")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get notification settings", description = "Get seller notification settings")
    public ResponseEntity<ApiResponse<SellerNotificationSettingsResponse>> getNotificationSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Notification settings retrieved", 
                sellerService.getNotificationSettings(sellerId)));
    }

    @PutMapping("/settings/notifications")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update notification settings", description = "Update seller notification settings")
    public ResponseEntity<ApiResponse<SellerNotificationSettingsResponse>> updateNotificationSettings(
            @RequestBody SellerNotificationSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Notification settings updated", 
                sellerService.updateNotificationSettings(sellerId, request)));
    }
}

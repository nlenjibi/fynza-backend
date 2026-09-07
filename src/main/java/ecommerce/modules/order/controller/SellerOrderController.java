package ecommerce.modules.order.controller;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.order.dto.SellerOrderStatsResponse;
import ecommerce.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Orders", description = "Seller order management")
public class SellerOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get seller orders")
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
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",
                orderService.getSellerOrders(principal.getId(), status, resolveDateFrom(dateFilter), null, search, pageable)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get seller order stats")
    public ResponseEntity<ApiResponse<SellerOrderStatsResponse>> getOrderStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Order stats retrieved successfully",
                orderService.getSellerOrderStats(principal.getId())));
    }

    @GetMapping("/export")
    @Operation(summary = "Export seller orders to CSV")
    public ResponseEntity<String> exportOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String search) {
        String csv = orderService.exportSellerOrdersToCSV(
                principal.getId(), status, resolveDateFrom(dateFilter), null, search);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=orders.csv")
                .body(csv);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully",
                orderService.updateSellerOrderStatus(id, request, principal.getId())));
    }

    private LocalDateTime resolveDateFrom(String dateFilter) {
        if (dateFilter == null) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (dateFilter.toLowerCase()) {
            case "today" -> now.toLocalDate().atStartOfDay();
            case "week"  -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year"  -> now.minusYears(1);
            default      -> null;
        };
    }
}

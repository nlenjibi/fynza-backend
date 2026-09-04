package ecommerce.modules.order.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.order.service.OrderService.CancelRequest;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * =================================================================
 * ORDER CONTROLLER
 * =================================================================
 * 
 * PURPOSE:
 * Handles HTTP requests for customer order operations.
 * Follows the strict Controller -> Service -> Repository -> Database layering.
 * 
 * LAYER: Controller (HTTP handling only, no business logic)
 * 
 * SECURITY:
 * - All endpoints require CUSTOMER role
 * - Users can only access their own orders
 * 
 * @author Fynza Backend Team
 * @version 2.1
 */
@RestController
@RequestMapping("api/v1/customers/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Orders", description = "Customer order management endpoints")
public class OrderController {

    private final OrderService orderService;

    /**
     * Retrieves all orders for the authenticated customer.
     * 
     * @param principal  The authenticated user principal
     * @param page       Page number (0-indexed)
     * @param size       Page size (default 10)
     * @param sortBy     Sort field (default createdAt)
     * @param direction  Sort direction (default DESC)
     * @return Paginated list of orders
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer orders", description = "Get all orders for the authenticated customer")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getCustomerOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponse> orders = orderService.getUserOrders(principal.getId(), pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", PaginatedResponse.from(orders)));
    }

    /**
     * Retrieves a specific order by ID for the authenticated customer.
     * 
     * @param id        The order UUID
     * @param principal The authenticated user principal
     * @return The order details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get order detail", description = "Get order detail by ID for the authenticated customer")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        OrderResponse order = orderService.getOrderById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    /**
     * Cancels an order for the authenticated customer.
     * 
     * @param id        The order UUID
     * @param request   The cancellation request containing reason
     * @param principal The authenticated user principal
     * @return The updated order
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel order", description = "Cancel an order by ID for the authenticated customer")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) OrderStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        String reason = request != null ? request.getNotes() : null;
        
        // Build cancel request with user info for activity tracking
        CancelRequest cancelRequest = CancelRequest.builder()
                .userId(principal.getId())
                .reason(reason)
                .build();
        
        OrderResponse order = orderService.cancelOrderWithTracking(id, cancelRequest);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }
}

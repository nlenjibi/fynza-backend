package ecommerce.modules.order.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderTimelineResponse;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.order.service.OrderService.CancelRequest;
import ecommerce.modules.order.service.OrderService.RefundRequest;
import ecommerce.modules.order.dto.OrderTrackingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * =================================================================
 * ORDER TRACKING CONTROLLER
 * =================================================================
 * 
 * PURPOSE:
 * Handles HTTP requests for order tracking and timeline operations.
 * Uses the consolidated OrderService for all operations.
 * 
 * LAYER: Controller (HTTP handling only, no business logic)
 * 
 * SECURITY:
 * - Tracking endpoints: CUSTOMER, ADMIN, or SELLER roles
 * - Cancel/Refund endpoints: CUSTOMER role only
 * 
 * @author Fynza Backend Team
 * @version 2.1
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderTrackingController {

    private final OrderService orderService;

    /**
     * Retrieves tracking information for an order.
     * 
     * @param orderId The order UUID
     * @return Tracking info including timeline
     */
    @GetMapping("/{orderId}/tracking")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getOrderTracking(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Tracking info retrieved successfully", 
                orderService.getTrackingInfo(orderId)));
    }

    /**
     * Retrieves the timeline of events for an order.
     * 
     * @param orderId The order UUID
     * @return List of timeline events
     */
    @GetMapping("/{orderId}/timeline")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<List<OrderTimelineResponse>>> getOrderTimeline(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success("Timeline retrieved successfully", 
                orderService.getOrderTimeline(orderId)));
    }

    /**
     * Cancels an order with activity tracking.
     * 
     * @param orderId The order UUID
     * @param request The cancellation request
     * @return The updated order
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID orderId, 
            @RequestBody(required = false) CancelRequest request) {
        
        // Use default values if request is null
        if (request == null) {
            request = new CancelRequest();
        }
        
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", 
                orderService.cancelOrderWithTracking(orderId, request)));
    }

    /**
     * Requests a refund for an order.
     * 
     * @param orderId The order UUID
     * @param request The refund request
     * @return The order response
     */
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> requestRefund(
            @PathVariable UUID orderId, 
            @RequestBody(required = false) RefundRequest request) {
        
        // Use default values if request is null
        if (request == null) {
            request = new RefundRequest();
        }
        
        return ResponseEntity.ok(ApiResponse.success("Refund requested successfully", 
                orderService.requestRefund(orderId, request)));
    }
}

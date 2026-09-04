package ecommerce.modules.order.service;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * =================================================================
 * ORDER SERVICE INTERFACE
 * =================================================================
 * 
 * PURPOSE:
 * This is the unified service interface for all order-related business operations.
 * All order functionality (CRUD, tracking, search, activities) is consolidated here
 * following the single responsibility principle at the service layer level.
 * 
 * LAYER: Service Interface (Business Logic Contract)
 * 
 * RESPONSIBILITIES:
 * - Order creation from cart/checkout
 * - Order retrieval (by ID, orderNumber, user)
 * - Order status management (cancel, update)
 * - Order tracking and timeline
 * - Order search with filters
 * - Order activity logging
 * - Order statistics
 * 
 * ARCHITECTURE NOTES:
 * - Controller -> Service -> Repository -> Database (strict layering)
 * - All transactional boundaries are defined in the implementation
 * - DTOs are used for all input/output (no entities exposed)
 * 
 * @author Fynza Backend Team
 * @version 2.1
 * @since 2026-03-14
 */
public interface OrderService {

    // =================================================================
    // ORDER CREATION OPERATIONS
    // =================================================================
    
    /**
     * Creates a new order from a cart.
     * 
     * Business Logic:
     * 1. Validates user and cart existence
     * 2. Validates shipping/billing addresses belong to user
     * 3. Applies coupon discounts if applicable
     * 4. Calculates subtotal, tax, shipping, and total
     * 5. Creates order items from cart items
     * 6. Releases reserved stock
     * 7. Clears the cart after successful order creation
     * 
     * @param request The order creation request containing address IDs and payment method
     * @param userId  The authenticated user's UUID
     * @return The created order response with all details
     */
    OrderResponse createOrder(CreateOrderRequest request, UUID userId);

    // =================================================================
    // ORDER RETRIEVAL OPERATIONS
    // =================================================================
    
    /**
     * Retrieves an order by its UUID.
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID (null for admin access)
     * @return The order response if found and authorized
     */
    OrderResponse getOrderById(UUID id, UUID userId);

    /**
     * Retrieves an order by its order number.
     * 
     * @param orderNumber The unique order number (e.g., ORD-20260314-ABC12345)
     * @param userId      The requesting user's UUID (null for admin access)
     * @return The order response if found and authorized
     */
    OrderResponse getOrderByOrderNumber(String orderNumber, UUID userId);

    /**
     * Retrieves all orders for a specific user with pagination.
     * 
     * @param userId   The customer's UUID
     * @param pageable Pagination parameters (page, size, sort)
     * @return A paginated list of order responses
     */
    Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable);

    // =================================================================
    // ORDER CANCELLATION OPERATIONS
    // =================================================================
    
    /**
     * Cancels an order (Admin operation - no user validation).
     * 
     * @param id     The order UUID
     * @param reason The cancellation reason
     * @return The updated order response
     */
    OrderResponse cancelOrder(UUID id, String reason);

    /**
     * Cancels an order by user (validates ownership).
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID
     * @return The updated order response
     */
    OrderResponse cancelOrder(UUID id, UUID userId);

    /**
     * Cancels an order by user with a reason (validates ownership).
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID
     * @param reason The cancellation reason
     * @return The updated order response
     */
    OrderResponse cancelOrder(UUID id, UUID userId, String reason);

    // =================================================================
    // ADMIN ORDER OPERATIONS
    // =================================================================
    
    /**
     * Retrieves all orders with pagination (Admin only).
     * 
     * @param pageable Pagination parameters
     * @return A paginated list of all orders
     */
    Page<OrderResponse> getAllOrders(Pageable pageable);

    /**
     * Retrieves all orders with a specific status (Admin only).
     * 
     * @param status  The order status to filter by
     * @param pageable Pagination parameters
     * @return A paginated list of orders with the specified status
     */
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Updates the status of an order (Admin only).
     * 
     * Business Logic:
     * - When status changes to CONFIRMED, payment status is set to PAID
     * 
     * @param id     The order UUID
     * @param status The new order status
     * @return The updated order response
     */
    OrderResponse updateOrderStatus(UUID id, OrderStatus status);

    /**
     * Updates order status and/or notes (Admin only).
     * 
     * @param id      The order UUID
     * @param request The update request containing status and/or notes
     * @return The updated order response
     */
    OrderResponse updateOrderStatus(UUID id, OrderStatusUpdateRequest request);

    /**
     * Retrieves order statistics (Admin only).
     * 
     * @return Statistics including counts by status
     */
    OrderStatsResponse getOrderStatistics();

    /**
     * Searches orders for admin with multiple filters.
     */
    Page<OrderResponse> searchOrdersAdmin(OrderSearchCriteria criteria, Pageable pageable);

    /**
     * Exports admin orders to CSV format.
     */
    String exportOrdersToCSV(OrderSearchCriteria criteria);


    // =================================================================
    // ORDER TRACKING OPERATIONS
    // =================================================================
    
    /**
     * Retrieves tracking information for an order.
     * 
     * @param orderId The order UUID
     * @return Complete tracking response including timeline
     */
    OrderTrackingResponse getTrackingInfo(UUID orderId);

    /**
     * Retrieves the timeline of events for an order.
     * 
     * @param orderId The order UUID
     * @return A list of timeline events (activities or legacy timeline entries)
     */
    List<OrderTimelineResponse> getOrderTimeline(UUID orderId);

    /**
     * Cancels an order with extended tracking support.
     * 
     * @param orderId The order UUID
     * @param request The cancellation request with reason and user info
     * @return The updated order response
     */
    OrderResponse cancelOrderWithTracking(UUID orderId, CancelRequest request);

    /**
     * Requests a refund for an order.
     * 
     * @param orderId The order UUID
     * @param request The refund request with reason and user info
     * @return The order response
     */
    OrderResponse requestRefund(UUID orderId, RefundRequest request);

    // =================================================================
    // ORDER SEARCH OPERATIONS
    // =================================================================
    
    /**
     * Searches orders for a customer with multiple filters.
     * 
     * Supported Filters:
     * - query: Search by order number
     * - status: Filter by order status
     * - dateFrom/dateTo: Date range filter
     * - minAmount/maxAmount: Amount range filter
     * - paymentMethod: Filter by payment method
     * 
     * @param customerId The customer's UUID
     * @param criteria   The search criteria
     * @param pageable   Pagination parameters
     * @return A paginated list of matching orders
     */
    Page<OrderResponse> searchOrders(UUID customerId, OrderSearchCriteria criteria, Pageable pageable);

    // =================================================================
    // SELLER ORDER OPERATIONS
    // =================================================================
    
    /**
     * Retrieves all orders for a specific seller with pagination.
     * 
     * @param sellerId The seller's UUID
     * @param pageable Pagination parameters
     * @return A paginated list of order responses
     */
    Page<OrderResponse> getSellerOrders(UUID sellerId, Pageable pageable);

    /**
     * Updates the status of an order for a specific seller.
     * Validates that the order contains items from this seller.
     * 
     * @param orderId   The order UUID
     * @param request   The status update request
     * @param sellerId  The seller's UUID
     * @return The updated order response
     */
    OrderResponse updateSellerOrderStatus(UUID orderId, OrderStatusUpdateRequest request, UUID sellerId);

    /**
     * Retrieves comprehensive order dashboard data for a seller.
     * 
     * @param sellerId The seller's UUID
     * @return Dashboard data including totals, recent orders, and revenue
     */
    SellerOrderDto getSellerOrderDashboard(UUID sellerId);

    /**
     * Retrieves order analytics for a seller.
     * 
     * @param sellerId The seller's UUID
     * @param days    Number of days to analyze
     * @return Analytics data including daily sales and top products
     */
    SellerOrderDto.SellerOrderAnalytics getSellerOrderAnalytics(UUID sellerId, int days);

    /**
     * Retrieves comprehensive analytics for seller dashboard.
     * 
     * @param sellerId The seller's UUID
     * @return Complete analytics including metrics, charts, and top items
     */
    ecommerce.modules.seller.dto.SellerAnalyticsDto getSellerAnalytics(UUID sellerId);

    /**
     * Retrieves comprehensive analytics for admin dashboard.
     * 
     * @param filterPeriod Filter period: day, week, month, year
     * @return Complete admin analytics including metrics, charts, and top items
     */
    ecommerce.modules.admin.dto.AdminAnalyticsDto getAdminAnalytics(String filterPeriod);

    /**
     * Retrieves seller orders with filters.
     */
    Page<OrderResponse> getSellerOrders(UUID sellerId, OrderStatus status, LocalDateTime dateFrom, 
            LocalDateTime dateTo, String query, Pageable pageable);

    /**
     * Retrieves order statistics for a seller.
     */
    SellerOrderStatsResponse getSellerOrderStats(UUID sellerId);

    /**
     * Exports seller orders to CSV format.
     */
    String exportSellerOrdersToCSV(UUID sellerId, OrderStatus status, LocalDateTime dateFrom,
            LocalDateTime dateTo, String query);

    // =================================================================
    // ADMIN DASHBOARD OPERATIONS
    // =================================================================
    
    /**
     * Retrieves order dashboard statistics for admin.
     * 
     * @return Dashboard data including total orders, status counts, recent orders, completion rate
     */
    OrderDashboardDto getOrderDashboard();

    /**
     * Retrieves recent orders for admin dashboard.
     * 
     * @param limit Number of recent orders to retrieve
     * @return List of recent order DTOs
     */
    List<OrderDashboardDto.RecentOrderDto> getRecentOrders(int limit);

    /**
     * Retrieves order counts grouped by status.
     * 
     * @return Map of status name to count
     */
    Map<String, Long> getOrdersByStatus();

    /**
     * Calculates total revenue from all orders.
     * 
     * @return Total revenue as BigDecimal
     */
    BigDecimal getTotalRevenue();

    // =================================================================
    // INNER CLASSES (Request DTOs for tracking operations)
    // =================================================================

    /**
     * Request DTO for order cancellation with extended tracking support.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class CancelRequest {
        private String reason;
        private UUID userId;
        private String ipAddress;
    }

    /**
     * Request DTO for refund requests with extended tracking support.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class RefundRequest {
        private String reason;
        private UUID userId;
        private String ipAddress;
    }

    /**
     * Search criteria DTO for customer order searches.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class OrderSearchCriteria {
        private String query;
        private String status;
        private String paymentStatus;
        private LocalDateTime dateFrom;
        private LocalDateTime dateTo;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String paymentMethod;
        private UUID sellerId;
    }
}

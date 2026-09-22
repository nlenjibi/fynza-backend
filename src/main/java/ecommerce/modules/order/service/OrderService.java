package ecommerce.modules.order.service;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.dto.request.CreateOrderRequest;
import ecommerce.modules.order.dto.request.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    // ── Customer ──────────────────────────────────────────────────────────────

    OrderResponse createOrder(UUID customerId, CreateOrderRequest request);

    OrderResponse getOrderById(UUID orderId, UUID customerId);

    OrderResponse getOrderByOrderNumber(String orderNumber, UUID customerId);

    Page<OrderResponse> getUserOrders(UUID customerId, Pageable pageable);

    void cancelOrder(UUID orderId, UUID customerId, String reason);

    // ── Admin ─────────────────────────────────────────────────────────────────

    Page<OrderResponse> getAllOrders(Pageable pageable);

    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    Page<OrderResponse> searchOrdersAdmin(String query, OrderStatus status, String paymentStatus, Pageable pageable);

    OrderResponse adminUpdateStatus(UUID orderId, UpdateOrderStatusRequest request);

    OrderStatsResponse getOrderStatistics();

    OrderDashboardDto getOrderDashboard();

    // ── Seller ────────────────────────────────────────────────────────────────

    Page<OrderResponse> getSellerOrders(UUID sellerId, Pageable pageable);

    void sellerUpdateStatus(UUID orderId, UUID sellerId, UpdateOrderStatusRequest request);

    SellerOrderStatsResponse getSellerOrderStats(UUID sellerId);

    // ── Shared reads ──────────────────────────────────────────────────────────

    List<OrderTimelineResponse> getOrderTimeline(UUID orderId);

    OrderTrackingResponse getTrackingInfo(UUID orderId);
}

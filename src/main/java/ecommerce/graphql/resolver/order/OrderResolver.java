package ecommerce.graphql.resolver.order;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.OrderResponseDto;
import ecommerce.graphql.input.*;
import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.order.service.OrderService.CancelRequest;
import ecommerce.modules.order.service.OrderService.OrderSearchCriteria;
import ecommerce.modules.order.service.OrderService.RefundRequest;
import ecommerce.modules.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderResolver {

    private final OrderService orderService;
    private final SellerService sellerService;

    // =========================================================================
    // CUSTOMER QUERIES
    // =========================================================================

    @QueryMapping
    public OrderResponse order(@Argument UUID id, @ContextValue UUID userId) {
        log.debug("GQL order(id={}, user={})", id, userId);
        return orderService.getOrderById(id, userId);
    }

    @QueryMapping
    public OrderResponse orderByNumber(@Argument String orderNumber,
                                       @ContextValue UUID userId) {
        log.debug("GQL orderByNumber({}, user={})", orderNumber, userId);
        return orderService.getOrderByOrderNumber(orderNumber, userId);
    }

    @QueryMapping
    public OrderResponseDto myOrders(@Argument PageInput pagination,
                                     @ContextValue UUID userId) {
        log.debug("GQL myOrders(user={})", userId);
        Page<OrderResponse> page = orderService.getUserOrders(userId, toPageable(pagination));
        return toOrderResponseDto(page);
    }

    // =========================================================================
    // ORDER TRACKING QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SELLER', 'STAFF', 'MANAGER')")
    public OrderTrackingResponse orderTracking(@Argument UUID orderId) {
        log.debug("GQL orderTracking(orderId={})", orderId);
        return orderService.getTrackingInfo(orderId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'SELLER', 'STAFF', 'MANAGER')")
    public List<OrderTimelineResponse> orderTimeline(@Argument UUID orderId) {
        log.debug("GQL orderTimeline(orderId={})", orderId);
        return orderService.getOrderTimeline(orderId);
    }

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto allOrders(@Argument PageInput pagination) {
        log.debug("GQL allOrders");
        Page<OrderResponse> page = orderService.getAllOrders(toPageable(pagination));
        return toOrderResponseDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto adminSearchOrders(@Argument OrderSearchInput filter,
                                               @Argument PageInput pagination) {
        log.debug("GQL adminSearchOrders(filter={})", filter);
        Pageable pageable = toPageable(pagination);
        OrderSearchCriteria criteria = toSearchCriteria(filter);
        Page<OrderResponse> page = orderService.searchOrdersAdmin(criteria, pageable);
        return toOrderResponseDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersByStatus(@Argument String status,
                                            @Argument PageInput pagination) {
        log.debug("GQL ordersByStatus(status={})", status);
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        Page<OrderResponse> page = orderService.getOrdersByStatus(orderStatus, toPageable(pagination));
        return toOrderResponseDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderStatsResponse orderStatistics() {
        log.debug("GQL orderStatistics");
        return orderService.getOrderStatistics();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderDashboardDto orderDashboard() {
        log.debug("GQL orderDashboard");
        return orderService.getOrderDashboard();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto customerOrders(@Argument UUID customerId,
                                            @Argument PageInput pagination) {
        log.debug("GQL customerOrders(customerId={})", customerId);
        Page<OrderResponse> page = orderService.getUserOrders(customerId, toPageable(pagination));
        return toOrderResponseDto(page);
    }

    // =========================================================================
    // SELLER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public OrderResponseDto sellerOrders(@Argument PageInput pagination,
                                          @Argument OrderSearchInput filter,
                                          @ContextValue UUID sellerId) {
        log.debug("GQL sellerOrders(seller={})", sellerId);
        Pageable pageable = toPageable(pagination);

        if (filter != null) {
            OrderSearchCriteria criteria = toSearchCriteria(filter);
            criteria.setSellerId(sellerId);
            Page<OrderResponse> page = orderService.searchOrders(
                    sellerId, criteria, pageable);
            return toOrderResponseDto(page);
        }

        Page<OrderResponse> page = orderService.getSellerOrders(sellerId, pageable);
        return toOrderResponseDto(page);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerOrderStatsResponse sellerOrderStats(@ContextValue UUID sellerId) {
        log.debug("GQL sellerOrderStats(seller={})", sellerId);
        return orderService.getSellerOrderStats(sellerId);
    }

    // =========================================================================
    // CUSTOMER MUTATIONS
    // =========================================================================

    @MutationMapping
    public OrderResponse createOrder(@Argument OrderCreateInput input,
                                     @ContextValue UUID userId) {
        log.info("GQL createOrder(user={})", userId);
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddressId(input.getShippingAddressId())
                .billingAddressId(input.getBillingAddressId())
                .paymentMethod(input.getPaymentMethod())
                .couponCode(input.getCouponCode())
                .build();
        return orderService.createOrder(request, userId);
    }

    @MutationMapping
    public OrderResponse cancelOrder(@Argument UUID id,
                                     @Argument String reason,
                                     @ContextValue UUID userId) {
        log.info("GQL cancelOrder(id={}, user={})", id, userId);
        return orderService.cancelOrder(id, userId, reason);
    }

    @MutationMapping
    public OrderResponse requestOrderRefund(@Argument UUID orderId,
                                             @Argument RefundOrderInput input,
                                             @ContextValue UUID userId) {
        log.info("GQL requestOrderRefund(orderId={}, user={})", orderId, userId);
        RefundRequest request = RefundRequest.builder()
                .reason(input != null ? input.getReason() : null)
                .userId(userId)
                .build();
        return orderService.requestRefund(orderId, request);
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse adminUpdateOrderStatus(@Argument UUID id,
                                                 @Argument OrderStatusUpdateInput input) {
        log.info("GQL adminUpdateOrderStatus(id={}, status={})", id, input.getStatus());
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.valueOf(input.getStatus().toUpperCase()))
                .trackingNumber(input.getTrackingNumber())
                .notes(input.getNotes())
                .build();
        return orderService.updateOrderStatus(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse adminCancelOrder(@Argument UUID id,
                                           @Argument String reason) {
        log.info("GQL adminCancelOrder(id={})", id);
        return orderService.cancelOrder(id, reason);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse confirmOrder(@Argument UUID id) {
        log.info("GQL confirmOrder(id={})", id);
        return orderService.updateOrderStatus(id, OrderStatus.CONFIRMED);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse processOrder(@Argument UUID id) {
        log.info("GQL processOrder(id={})", id);
        return orderService.updateOrderStatus(id, OrderStatus.PROCESSING);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse shipOrder(@Argument UUID id,
                                   @Argument String trackingNumber,
                                   @Argument String carrier) {
        log.info("GQL shipOrder(id={}, tracking={})", id, trackingNumber);
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .trackingNumber(trackingNumber)
                .notes(carrier != null ? "Carrier: " + carrier : null)
                .build();
        return orderService.updateOrderStatus(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse deliverOrder(@Argument UUID id) {
        log.info("GQL deliverOrder(id={})", id);
        return orderService.updateOrderStatus(id, OrderStatus.DELIVERED);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public OrderResponse refundOrder(@Argument UUID id,
                                     @Argument BigDecimal amount,
                                     @Argument String reason) {
        log.info("GQL refundOrder(id={}, amount={})", id, amount);
        RefundRequest request = RefundRequest.builder()
                .reason(reason)
                .build();
        return orderService.requestRefund(id, request);
    }

    // =========================================================================
    // SELLER MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public OrderResponse sellerUpdateOrderStatus(@Argument UUID orderId,
                                                  @Argument OrderStatusUpdateInput input,
                                                  @ContextValue UUID sellerId) {
        log.info("GQL sellerUpdateOrderStatus(orderId={}, seller={})", orderId, sellerId);
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.valueOf(input.getStatus().toUpperCase()))
                .trackingNumber(input.getTrackingNumber())
                .notes(input.getNotes())
                .build();
        return orderService.updateSellerOrderStatus(orderId, request, sellerId);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    private OrderResponseDto toOrderResponseDto(Page<OrderResponse> page) {
        return OrderResponseDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    private OrderSearchCriteria toSearchCriteria(OrderSearchInput input) {
        if (input == null) {
            return OrderSearchCriteria.builder().build();
        }
        return OrderSearchCriteria.builder()
                .query(input.getQuery())
                .status(input.getStatus())
                .paymentStatus(input.getPaymentStatus())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .minAmount(input.getMinAmount())
                .maxAmount(input.getMaxAmount())
                .build();
    }
}

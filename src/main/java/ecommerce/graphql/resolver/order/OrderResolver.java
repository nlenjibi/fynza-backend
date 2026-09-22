package ecommerce.graphql.resolver.order;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.OrderResponseDto;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderResolver {

    private final OrderService orderService;

    // ── Customer ──────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderResponse order(@Argument UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return orderService.getOrderById(id, principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderResponse orderByNumber(@Argument String orderNumber,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return orderService.getOrderByOrderNumber(orderNumber, principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderResponseDto myOrders(@Argument PageInput pagination,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return wrap(orderService.getUserOrders(principal.getId(), toPageable(pagination)));
    }

    // ── Tracking ──────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderTrackingResponse orderTracking(@Argument UUID orderId) {
        return orderService.getTrackingInfo(orderId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<OrderTimelineResponse> orderTimeline(@Argument UUID orderId) {
        return orderService.getOrderTimeline(orderId);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto allOrders(@Argument PageInput pagination) {
        return wrap(orderService.getAllOrders(toPageable(pagination)));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto adminSearchOrders(@Argument String query,
                                              @Argument String status,
                                              @Argument String paymentStatus,
                                              @Argument PageInput pagination) {
        OrderStatus os = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;
        return wrap(orderService.searchOrdersAdmin(query, os, paymentStatus, toPageable(pagination)));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersByStatus(@Argument String status, @Argument PageInput pagination) {
        return wrap(orderService.getOrdersByStatus(OrderStatus.valueOf(status.toUpperCase()), toPageable(pagination)));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderStatsResponse orderStatistics() {
        return orderService.getOrderStatistics();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderDashboardDto orderDashboard() {
        return orderService.getOrderDashboard();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto customerOrders(@Argument UUID customerId, @Argument PageInput pagination) {
        return wrap(orderService.getUserOrders(customerId, toPageable(pagination)));
    }

    // ── Seller ────────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public OrderResponseDto sellerOrders(@Argument PageInput pagination,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return wrap(orderService.getSellerOrders(principal.getId(), toPageable(pagination)));
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerOrderStatsResponse sellerOrderStats(@AuthenticationPrincipal UserPrincipal principal) {
        return orderService.getSellerOrderStats(principal.getId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Pageable toPageable(PageInput input) {
        if (input == null) return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    private OrderResponseDto wrap(Page<OrderResponse> page) {
        return OrderResponseDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }
}

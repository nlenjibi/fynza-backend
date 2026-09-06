package ecommerce.graphql.resolver.order;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.OrderResponseDto;
import ecommerce.graphql.input.*;
import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.order.service.OrderService.OrderSearchCriteria;
import ecommerce.modules.seller.service.SellerService;
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
    private final SellerService sellerService;

    // =========================================================================
    // CUSTOMER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderResponse order(@Argument UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL order(id={}, user={})", id, principal.getId());
        return orderService.getOrderById(id, principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderResponse orderByNumber(@Argument String orderNumber,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL orderByNumber({}, user={})", orderNumber, principal.getId());
        return orderService.getOrderByOrderNumber(orderNumber, principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public OrderResponseDto myOrders(@Argument PageInput pagination,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myOrders(user={})", principal.getId());
        return toOrderResponseDto(orderService.getUserOrders(principal.getId(), toPageable(pagination)));
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
        return toOrderResponseDto(orderService.getAllOrders(toPageable(pagination)));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto adminSearchOrders(@Argument OrderSearchInput filter,
                                              @Argument PageInput pagination) {
        log.debug("GQL adminSearchOrders(filter={})", filter);
        return toOrderResponseDto(orderService.searchOrdersAdmin(toSearchCriteria(filter), toPageable(pagination)));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto ordersByStatus(@Argument String status, @Argument PageInput pagination) {
        log.debug("GQL ordersByStatus(status={})", status);
        return toOrderResponseDto(orderService.getOrdersByStatus(OrderStatus.valueOf(status.toUpperCase()), toPageable(pagination)));
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
    public OrderResponseDto customerOrders(@Argument UUID customerId, @Argument PageInput pagination) {
        log.debug("GQL customerOrders(customerId={})", customerId);
        return toOrderResponseDto(orderService.getUserOrders(customerId, toPageable(pagination)));
    }

    // =========================================================================
    // SELLER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public OrderResponseDto sellerOrders(@Argument PageInput pagination,
                                         @Argument OrderSearchInput filter,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL sellerOrders(seller={})", principal.getId());
        Pageable pageable = toPageable(pagination);
        if (filter != null) {
            OrderSearchCriteria criteria = toSearchCriteria(filter);
            criteria.setSellerId(principal.getId());
            return toOrderResponseDto(orderService.searchOrders(principal.getId(), criteria, pageable));
        }
        return toOrderResponseDto(orderService.getSellerOrders(principal.getId(), pageable));
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerOrderStatsResponse sellerOrderStats(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL sellerOrderStats(seller={})", principal.getId());
        return orderService.getSellerOrderStats(principal.getId());
    }

    // Order mutations are REST-only per PRD §19 & §86.
    // Use: POST /v1/checkout, POST /v1/orders/{id}/cancel, POST /v1/orders/{id}/refund

    // =========================================================================
    // HELPERS
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
        if (input == null) return OrderSearchCriteria.builder().build();
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

package ecommerce.modules.order.service.impl;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.entity.CartItem;
import ecommerce.modules.cart.entity.CartStatus;
import ecommerce.modules.cart.repository.CartRepository;
import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.dto.request.CreateOrderRequest;
import ecommerce.modules.order.dto.request.UpdateOrderStatusRequest;
import ecommerce.modules.order.entity.*;
import ecommerce.modules.order.event.OrderCancelledEvent;
import ecommerce.modules.order.event.OrderPlacedEvent;
import ecommerce.modules.order.event.OrderStatusChangedEvent;
import ecommerce.modules.order.exception.InvalidOrderStateException;
import ecommerce.modules.order.exception.OrderAccessDeniedException;
import ecommerce.modules.order.exception.OrderNotFoundException;
import ecommerce.modules.order.repository.*;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.user.entity.Address;
import ecommerce.modules.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal TAX_RATE      = new BigDecimal("0.05");
    private static final BigDecimal SHIPPING_FLAT = new BigDecimal("15.00");

    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED
    );

    private final OrderRepository          orderRepository;
    private final OrderItemRepository      orderItemRepository;
    private final SellerOrderRepository    sellerOrderRepository;
    private final OrderTimelineRepository  orderTimelineRepository;
    private final CartRepository           cartRepository;
    private final ProductRepository        productRepository;
    private final AddressRepository        addressRepository;
    private final SellerRepository         sellerRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Customer ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse createOrder(UUID customerId, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(customerId)
                .orElseThrow(() -> new BadRequestException("No active cart found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        OrderAddress shipping = resolveAddress(request.getShippingAddressId(), customerId);
        OrderAddress billing  = resolveAddress(request.getBillingAddressId(),  customerId);
        if (billing == null) billing = shipping;

        // Snapshot products in one pass
        Map<UUID, Product> productMap = new LinkedHashMap<>();
        for (CartItem ci : cart.getItems()) {
            productMap.computeIfAbsent(ci.getProductId(),
                    id -> productRepository.findById(id)
                            .orElseThrow(() -> new BadRequestException("Product not found: " + id)));
        }

        // Pricing from cart totals (validated by cart module)
        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal discount = cart.getDiscountAmount();
        BigDecimal tax      = subtotal.subtract(discount).multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping_cost = subtotal.subtract(discount).compareTo(new BigDecimal("200")) >= 0
                ? BigDecimal.ZERO : SHIPPING_FLAT;
        BigDecimal total = subtotal.subtract(discount).add(tax).add(shipping_cost);

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .discount(discount)
                .tax(tax)
                .shippingCost(shipping_cost)
                .totalAmount(total)
                .shippingAddress(shipping)
                .billingAddress(billing)
                .couponCode(cart.getCouponCode())
                .customerNotes(request.getCustomerNotes())
                .build();

        Order saved = orderRepository.save(order);

        // Group cart items by seller, create SellerOrder + OrderItem per seller
        Map<Long, List<CartItem>> bySeller = new LinkedHashMap<>();
        for (CartItem ci : cart.getItems()) {
            Long sellerId = productMap.get(ci.getProductId()).getSellerId();
            bySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(ci);
        }

        for (var entry : bySeller.entrySet()) {
            Long sellerId    = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();

            BigDecimal sellerSubtotal = sellerItems.stream()
                    .map(CartItem::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            SellerOrder sellerOrder = SellerOrder.builder()
                    .order(saved)
                    .sellerId(sellerId)
                    .status(OrderStatus.PENDING)
                    .subtotal(sellerSubtotal)
                    .build();
            SellerOrder savedSO = sellerOrderRepository.save(sellerOrder);

            for (CartItem ci : sellerItems) {
                Product product = productMap.get(ci.getProductId());
                OrderItem item = OrderItem.builder()
                        .order(saved)
                        .sellerOrder(savedSO)
                        .productId(ci.getProductId())
                        .variantId(ci.getVariantId())
                        .sellerId(sellerId)
                        .productName(product.getName())
                        .productSku(product.getSku())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .subtotal(ci.getLineTotal())
                        .build();
                orderItemRepository.save(item);
            }
        }

        // Record first timeline entry
        recordTimeline(saved, OrderStatus.PENDING, "Order placed", customerId);

        // Clear cart
        cart.clearItems();
        cart.setCouponCode(null);
        cart.setStatus(CartStatus.CHECKOUT);
        cartRepository.save(cart);

        eventPublisher.publishEvent(
                new OrderPlacedEvent(saved.getPublicId(), orderNumber, customerId, total));

        log.info("Order {} created for customer {}", orderNumber, customerId);
        return toResponse(saved);
    }

    @Override
    public OrderResponse getOrderById(UUID orderId, UUID customerId) {
        Order order = findByPublicId(orderId);
        if (customerId != null && !customerId.equals(order.getCustomerId())) {
            throw new OrderAccessDeniedException();
        }
        return toResponse(order);
    }

    @Override
    public OrderResponse getOrderByOrderNumber(String orderNumber, UUID customerId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        if (customerId != null && !customerId.equals(order.getCustomerId())) {
            throw new OrderAccessDeniedException();
        }
        return toResponse(order);
    }

    @Override
    public Page<OrderResponse> getUserOrders(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId, UUID customerId, String reason) {
        Order order = findByPublicId(orderId);
        if (!customerId.equals(order.getCustomerId())) throw new OrderAccessDeniedException();
        assertCancellable(order);

        OrderStatus prev = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        recordTimeline(order, OrderStatus.CANCELLED,
                "Cancelled by customer" + (reason != null ? ": " + reason : ""), customerId);

        eventPublisher.publishEvent(
                new OrderCancelledEvent(order.getPublicId(), order.getOrderNumber(), customerId, reason));

        log.info("Order {} cancelled by customer {}", order.getOrderNumber(), customerId);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Override
    public Page<OrderResponse> searchOrdersAdmin(String query, OrderStatus status, String paymentStatus, Pageable pageable) {
        PaymentStatus ps = paymentStatus != null ? PaymentStatus.valueOf(paymentStatus.toUpperCase()) : null;
        return orderRepository.searchAdmin(status, ps, query, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse adminUpdateStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = findByPublicId(orderId);
        OrderStatus prev = order.getStatus();
        order.setStatus(request.getStatus());
        if (request.getTrackingNumber() != null) order.setTrackingNumber(request.getTrackingNumber());
        if (request.getStatus() == OrderStatus.CONFIRMED) order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        recordTimeline(order, request.getStatus(),
                request.getNotes() != null ? request.getNotes() : "Status updated by admin", null);

        eventPublisher.publishEvent(
                new OrderStatusChangedEvent(order.getPublicId(), order.getOrderNumber(),
                        order.getCustomerId(), prev, request.getStatus()));

        return toResponse(order);
    }

    @Override
    public OrderStatsResponse getOrderStatistics() {
        long total     = orderRepository.count();
        long pending   = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long processing= orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shipped   = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        BigDecimal revenue = orderRepository.sumTotalRevenue();
        BigDecimal avg     = total > 0 ? orderRepository.avgOrderValue() : BigDecimal.ZERO;

        return OrderStatsResponse.builder()
                .totalOrders(total)
                .pendingOrders(pending)
                .confirmedOrders(confirmed)
                .processingOrders(processing)
                .shippedOrders(shipped)
                .deliveredOrders(delivered)
                .cancelledOrders(cancelled)
                .totalRevenue(revenue)
                .averageOrderValue(avg)
                .build();
    }

    @Override
    public OrderDashboardDto getOrderDashboard() {
        long total   = orderRepository.count();
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        BigDecimal revenue = orderRepository.sumTotalRevenue();

        List<Order> recent = orderRepository.findRecentOrders(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<OrderDashboardDto.RecentOrderDto> recentDtos = recent.stream()
                .map(o -> OrderDashboardDto.RecentOrderDto.builder()
                        .orderId(o.getPublicId().toString())
                        .orderNumber(o.getOrderNumber())
                        .amount(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .createdAt(o.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        double completionRate = total > 0
                ? (double) orderRepository.countByStatus(OrderStatus.DELIVERED) / total * 100 : 0;
        double cancellationRate = total > 0
                ? (double) orderRepository.countByStatus(OrderStatus.CANCELLED) / total * 100 : 0;

        return OrderDashboardDto.builder()
                .totalOrders(total)
                .pendingOrders(pending)
                .recentOrders(recentDtos)
                .totalRevenue(revenue)
                .orderCompletionRate(completionRate)
                .cancellationRate(cancellationRate)
                .build();
    }

    // ── Seller ────────────────────────────────────────────────────────────────

    @Override
    public Page<OrderResponse> getSellerOrders(UUID sellerId, Pageable pageable) {
        // Convert UUID sellerId to Long — seller identity in this platform is Long-based
        // We retrieve SellerOrders by sellerId (Long) mapped from product.sellerId
        // For GraphQL context, sellerId comes from UserPrincipal which is UUID-based
        // We query SellerOrder and map back to parent Orders
        Long sellerLongId = resolveSellerLongId(sellerId);
        return sellerOrderRepository.findBySellerIdOrderByCreatedAtDesc(sellerLongId, pageable)
                .map(so -> toResponse(so.getOrder()));
    }

    @Override
    @Transactional
    public void sellerUpdateStatus(UUID orderId, UUID sellerId, UpdateOrderStatusRequest request) {
        Long sellerLongId = resolveSellerLongId(sellerId);
        SellerOrder so = sellerOrderRepository.findBySellerIdAndOrderPublicId(sellerLongId, orderId)
                .orElseThrow(() -> new OrderNotFoundException("Seller order not found"));
        so.setStatus(request.getStatus());
        if (request.getTrackingNumber() != null) so.setTrackingNumber(request.getTrackingNumber());
        sellerOrderRepository.save(so);
    }

    @Override
    public SellerOrderStatsResponse getSellerOrderStats(UUID sellerId) {
        Long sellerLongId = resolveSellerLongId(sellerId);
        return SellerOrderStatsResponse.builder()
                .totalOrders(sellerOrderRepository.countBySellerId(sellerLongId))
                .pending(sellerOrderRepository.countBySellerIdAndStatus(sellerLongId, OrderStatus.PENDING))
                .confirmed(sellerOrderRepository.countBySellerIdAndStatus(sellerLongId, OrderStatus.CONFIRMED))
                .processing(sellerOrderRepository.countBySellerIdAndStatus(sellerLongId, OrderStatus.PROCESSING))
                .shipped(sellerOrderRepository.countBySellerIdAndStatus(sellerLongId, OrderStatus.SHIPPED))
                .delivered(sellerOrderRepository.countBySellerIdAndStatus(sellerLongId, OrderStatus.DELIVERED))
                .cancelled(sellerOrderRepository.countBySellerIdAndStatus(sellerLongId, OrderStatus.CANCELLED))
                .build();
    }

    // ── Shared reads ──────────────────────────────────────────────────────────

    @Override
    public List<OrderTimelineResponse> getOrderTimeline(UUID orderId) {
        Order order = findByPublicId(orderId);
        return orderTimelineRepository.findByOrder_IdOrderByCreatedAtDesc(order.getId())
                .stream()
                .map(tl -> OrderTimelineResponse.builder()
                        .id(tl.getPublicId())
                        .status(tl.getStatus().name())
                        .message(tl.getMessage())
                        .changedBy(tl.getChangedBy())
                        .occurredAt(tl.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public OrderTrackingResponse getTrackingInfo(UUID orderId) {
        Order order = findByPublicId(orderId);
        List<OrderTimelineResponse> timeline = getOrderTimeline(orderId);
        return OrderTrackingResponse.builder()
                .orderId(order.getPublicId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .displayName(order.getStatus().getDisplayName())
                .trackingNumber(order.getTrackingNumber())
                .timeline(timeline)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order findByPublicId(UUID publicId) {
        return orderRepository.findByPublicId(publicId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + publicId));
    }

    private void assertCancellable(Order order) {
        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order in status " + order.getStatus() + " cannot be cancelled");
        }
    }

    private void recordTimeline(Order order, OrderStatus status, String message, UUID changedBy) {
        OrderTimeline entry = OrderTimeline.builder()
                .order(order)
                .status(status)
                .message(message)
                .changedBy(changedBy)
                .build();
        orderTimelineRepository.save(entry);
    }

    private OrderAddress resolveAddress(UUID addressId, UUID customerId) {
        if (addressId == null) return null;
        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new BadRequestException("Address not found: " + addressId));
        if (!address.getUser().getPublicId().equals(customerId)) {
            throw new OrderAccessDeniedException();
        }
        return OrderAddress.from(address);
    }

    private String generateOrderNumber() {
        int year = Year.now().getValue();
        // Retry loop guards against a (very unlikely) race on the same ms
        for (int attempt = 0; attempt < 10; attempt++) {
            long seq = System.nanoTime() % 1_000_000;
            String candidate = String.format("FYN-%d-%06d", year, Math.abs(seq));
            if (!orderRepository.existsByOrderNumber(candidate)) return candidate;
        }
        return "FYN-" + year + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrder_PublicId(order.getPublicId())
                .stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getPublicId())
                        .productId(i.getProductId())
                        .variantId(i.getVariantId())
                        .sellerId(i.getSellerId())
                        .productName(i.getProductName())
                        .productSku(i.getProductSku())
                        .productImageUrl(i.getProductImageUrl())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .createdAt(i.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getPublicId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .customerId(order.getCustomerId())
                .items(items)
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shippingCost(order.getShippingCost())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .trackingNumber(order.getTrackingNumber())
                .customerNotes(order.getCustomerNotes())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private Long resolveSellerLongId(UUID ownerUserId) {
        return sellerRepository.findByOwnerUserId(ownerUserId)
                .map(s -> s.getId())
                .orElse(0L);
    }
}

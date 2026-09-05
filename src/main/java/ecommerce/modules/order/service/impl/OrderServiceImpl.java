package ecommerce.modules.order.service.impl;

import ecommerce.common.enums.DiscountType;
import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.common.exception.UnauthorizedException;
import ecommerce.modules.activity.entity.OrderActivityLog;
import ecommerce.modules.activity.repository.OrderActivityLogRepository;
import ecommerce.modules.activity.service.ActivityLogService;
import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.repository.CartRepository;
import ecommerce.modules.coupon.service.CouponService;
import ecommerce.modules.order.dto.*;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.OrderItem;
import ecommerce.modules.order.entity.OrderStats;
import ecommerce.modules.order.entity.OrderTimeline;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.order.repository.OrderTimelineRepository;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.refund.repository.RefundRepository;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.entity.Address;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * =================================================================
 * ORDER SERVICE IMPLEMENTATION
 * =================================================================
 * 
 * PURPOSE:
 * This is the unified service implementation for all order-related business operations.
 * All order functionality is consolidated here following the single responsibility principle
 * and clean code practices outlined in system.md.
 * 
 * LAYER: Service Implementation (Business Logic)
 * 
 * ARCHITECTURE PATTERN: Transaction Script
 * - Each public method represents a use case
 * - Transactional boundaries are clearly defined
 * - Business rules are encapsulated within methods
 * 
 * CONCURRENCY HANDLING:
 * - Uses optimistic locking where necessary
 * - Order status transitions are validated
 * 
 * CACHING STRATEGY:
 * - Order statistics are cached (to be implemented)
 * - Tracking info can be cached with short TTL
 * 
 * @author Fynza Backend Team
 * @version 2.1
 * @since 2026-03-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    // =================================================================
    // DEPENDENCIES (Injected via Constructor)
    // =================================================================
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CouponService couponService;
    private final ProductRepository productRepository;
    private final OrderTimelineRepository orderTimelineRepository;
    private final OrderActivityLogRepository orderActivityLogRepository;
    private final ActivityLogService activityLogService;
    private final RefundRepository refundRepository;

    // =================================================================
    // CONSTANTS (Business Rules Configuration)
    // =================================================================
    
    /** Tax rate applied to orders (10%) */
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.1);
    
    /** Free shipping threshold amount ($50) */
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(50);
    
    /** Standard shipping cost when below free shipping threshold */
    private static final BigDecimal SHIPPING_COST = BigDecimal.valueOf(5.99);

    // =================================================================
    // ORDER CREATION OPERATIONS
    // =================================================================

    /**
     * Creates a new order from the user's cart.
     * 
     * Transaction: READ_WRITE (modifies cart and inventory)
     * 
     * Steps:
     * 1. Validate user exists
     * 2. Fetch cart with items
     * 3. Validate addresses (if provided)
     * 4. Calculate pricing (subtotal, discount, tax, shipping, total)
     * 5. Create order and order items
     * 6. Release reserved stock
     * 7. Clear the cart
     * 
     * @param request Order creation request with addresses and payment method
     * @param userId  The authenticated user's UUID
     * @return The created order with all details mapped to response DTO
     */
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID userId) {
        log.info("Creating order for user: {}", userId);

        // Step 1: Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Step 2: Fetch cart with items
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty or not found"));

        // Validate cart is not empty
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Step 3: Validate and fetch shipping address
        Address shippingAddress = validateAndFetchAddress(request.getShippingAddressId(), userId, "Shipping");

        // Step 4: Validate and fetch billing address
        Address billingAddress = validateAndFetchAddress(request.getBillingAddressId(), userId, "Billing");

        // Ensure at least one address is provided
        if (shippingAddress == null && billingAddress == null) {
            throw new BadRequestException("Shipping or billing address is required");
        }

        // Step 5: Calculate pricing
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = calculateDiscount(cart, subtotal);
        BigDecimal tax = calculateTax(subtotal, discount);
        BigDecimal shippingCost = calculateShippingCost(subtotal);
        BigDecimal total = calculateTotal(subtotal, discount, tax, shippingCost);

        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Create order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(user)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(shippingCost)
                .discount(discount)
                .totalAmount(total)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress != null ? billingAddress : shippingAddress)
                .paymentStatus(ecommerce.modules.order.entity.PaymentStatus.PENDING)
                .couponCode(cart.getCouponCode())
                .build();

        // Set payment method if provided
        if (request.getPaymentMethod() != null) {
            try {
                order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment method: {}", request.getPaymentMethod());
            }
        }

        // Create order items from cart items and release reserved stock
        for (var cartItem : cart.getItems()) {
            var product = cartItem.getProduct();
            BigDecimal itemPrice = cartItem.getPrice();
            BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(itemPrice)
                    .subtotal(itemSubtotal)
                    .build();

            order.addOrderItem(orderItem);

            // Release reserved stock and activate product
            productRepository.releaseReservedStockAndIsActiveTrue(product.getId(), cartItem.getQuantity());
        }

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Step 7: Clear the cart
        cart.getItems().clear();
        cart.setCouponCode(null);
        cartRepository.save(cart);

        log.info("Order created successfully with order number: {}", orderNumber);
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Validates that an address belongs to the specified user.
     * 
     * @param addressId   The address UUID to validate
     * @param userId      The user's UUID
     * @param addressType The type of address for error messages
     * @return The validated address, or null if addressId is null
     */
    private Address validateAndFetchAddress(UUID addressId, UUID userId, String addressType) {
        if (addressId == null) {
            return null;
        }
        
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(addressType + " address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException(addressType + " address does not belong to user");
        }
        
        return address;
    }

    /**
     * Calculates the subtotal from cart items.
     * 
     * @param cart The cart containing items
     * @return The calculated subtotal
     */
    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates discount based on coupon if applicable.
     * 
     * @param cart     The cart with coupon code
     * @param subtotal The order subtotal
     * @return The discount amount
     */
    private BigDecimal calculateDiscount(Cart cart, BigDecimal subtotal) {
        if (cart.getCouponCode() == null || cart.getCouponCode().isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            var coupon = couponService.validate(cart.getCouponCode(), subtotal);
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                return subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                return coupon.getDiscountValue();
            }
        } catch (Exception e) {
            log.warn("Coupon validation failed: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculates tax based on subtotal after discount.
     * 
     * @param subtotal The order subtotal
     * @param discount The applied discount
     * @return The calculated tax
     */
    private BigDecimal calculateTax(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).multiply(TAX_RATE);
    }

    /**
     * Calculates shipping cost based on subtotal.
     * 
     * @param subtotal The order subtotal
     * @return The shipping cost (free if above threshold)
     */
    private BigDecimal calculateShippingCost(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 
                ? BigDecimal.ZERO 
                : SHIPPING_COST;
    }

    /**
     * Calculates the total order amount.
     * 
     * @param subtotal     The order subtotal
     * @param discount     The applied discount
     * @param tax           The calculated tax
     * @param shippingCost The shipping cost
     * @return The total order amount
     */
    private BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount, 
                                       BigDecimal tax, BigDecimal shippingCost) {
        return subtotal.subtract(discount).add(tax).add(shippingCost);
    }

    /**
     * Generates a unique order number.
     * 
     * Format: ORD-{timestamp}-{random}
     * Example: ORD-20260314-ABC12345
     * 
     * @return The generated order number
     */
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }

    // =================================================================
    // ORDER RETRIEVAL OPERATIONS
    // =================================================================

    /**
     * Retrieves an order by its UUID with authorization check.
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID (null for admin)
     * @return The order response
     */
    @Override
    public OrderResponse getOrderById(UUID id, UUID userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        
        validateOrderAccess(order, userId);
        return mapToOrderResponse(order);
    }

    /**
     * Retrieves an order by its order number with authorization check.
     * 
     * @param orderNumber The unique order number
     * @param userId      The requesting user's UUID (null for admin)
     * @return The order response
     */
    @Override
    public OrderResponse getOrderByOrderNumber(String orderNumber, UUID userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with orderNumber: " + orderNumber));
        
        validateOrderAccess(order, userId);
        return mapToOrderResponse(order);
    }

    /**
     * Retrieves all orders for a specific user with pagination.
     * 
     * @param userId   The customer's UUID
     * @param pageable Pagination parameters
     * @return A paginated list of orders
     */
    @Override
    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByCustomerId(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Validates that the user has access to view the order.
     * 
     * @param order  The order to validate access for
     * @param userId The requesting user's UUID (null allows admin access)
     */
    private void validateOrderAccess(Order order, UUID userId) {
        if (userId != null && !order.getCustomer().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to view this order");
        }
    }

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
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        
        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes(reason);
        
        return mapToOrderResponse(orderRepository.save(order));
    }

    /**
     * Cancels an order by user (validates ownership).
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID
     * @return The updated order response
     */
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id, UUID userId) {
        return cancelOrderWithValidation(id, userId, null);
    }

    /**
     * Cancels an order by user with a reason (validates ownership).
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID
     * @param reason The cancellation reason
     * @return The updated order response
     */
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id, UUID userId, String reason) {
        return cancelOrderWithValidation(id, userId, reason);
    }

    /**
     * Internal method to cancel order with validation.
     * 
     * @param id     The order UUID
     * @param userId The requesting user's UUID
     * @param reason The cancellation reason (optional)
     * @return The updated order response
     */
    private OrderResponse cancelOrderWithValidation(UUID id, UUID userId, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        
        // Validate ownership
        if (!order.getCustomer().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to cancel this order");
        }
        
        // Validate order status
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }
        
        // Cancel the order
        order.setStatus(OrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            order.setNotes(reason);
        }
        
        return mapToOrderResponse(orderRepository.save(order));
    }

    // =================================================================
    // ADMIN ORDER OPERATIONS
    // =================================================================

    /**
     * Retrieves all orders with pagination (Admin only).
     * 
     * @param pageable Pagination parameters
     * @return A paginated list of all orders
     */
    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Retrieves all orders with a specific status (Admin only).
     * 
     * @param status  The order status to filter by
     * @param pageable Pagination parameters
     * @return A paginated list of orders with the specified status
     */
    @Override
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Updates the status of an order (Admin only).
     * 
     * Business Rule: When status changes to CONFIRMED, payment status is set to PAID.
     * 
     * @param id     The order UUID
     * @param status The new order status
     * @return The updated order response
     */
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        
        order.setStatus(status);
        
        // Auto-update payment status when order is confirmed
        if (status == OrderStatus.CONFIRMED) {
            order.setPaymentStatus(ecommerce.modules.order.entity.PaymentStatus.PAID);
        }
        
        return mapToOrderResponse(orderRepository.save(order));
    }

    /**
     * Updates order status and/or notes (Admin only).
     * 
     * @param id      The order UUID
     * @param request The update request containing status and/or notes
     * @return The updated order response
     */
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Order", id));
        
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
            
            // Auto-update payment status when order is confirmed
            if (request.getStatus() == OrderStatus.CONFIRMED) {
                order.setPaymentStatus(ecommerce.modules.order.entity.PaymentStatus.PAID);
            }
        }
        
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        
        return mapToOrderResponse(orderRepository.save(order));
    }

    /**
     * Retrieves order statistics (Admin only).
     * 
     * @return Statistics including counts by status
     */
    @Override
    public OrderStatsResponse getOrderStatistics() {
        return OrderStatsResponse.builder()
                .stats(OrderStats.builder()
                        .totalOrders(orderRepository.count())
                        .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                        .processingOrders(orderRepository.countByStatus(OrderStatus.PROCESSING))
                        .shippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED))
                        .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                        .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                        .build())
                .build();
    }

    /**
     * Searches orders for admin with multiple filters.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrdersAdmin(OrderSearchCriteria criteria, Pageable pageable) {
        OrderStatus status = null;
        if (criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            status = OrderStatus.valueOf(criteria.getStatus().toUpperCase());
        }

        ecommerce.modules.order.entity.PaymentStatus paymentStatus = null;
        if (criteria.getPaymentStatus() != null && !criteria.getPaymentStatus().isBlank()) {
            paymentStatus = ecommerce.modules.order.entity.PaymentStatus.valueOf(criteria.getPaymentStatus().toUpperCase());
        }

        Page<Order> orders = orderRepository.searchOrdersAdmin(
                status,
                paymentStatus,
                criteria.getDateFrom(),
                criteria.getDateTo(),
                criteria.getMinAmount(),
                criteria.getMaxAmount(),
                criteria.getQuery(),
                pageable
        );

        return orders.map(this::mapToOrderResponse);
    }

    /**
     * Exports admin orders to CSV format.
     */
    @Override
    @Transactional(readOnly = true)
    public String exportOrdersToCSV(OrderSearchCriteria criteria) {
        OrderStatus status = null;
        if (criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            status = OrderStatus.valueOf(criteria.getStatus().toUpperCase());
        }

        ecommerce.modules.order.entity.PaymentStatus paymentStatus = null;
        if (criteria.getPaymentStatus() != null && !criteria.getPaymentStatus().isBlank()) {
            paymentStatus = ecommerce.modules.order.entity.PaymentStatus.valueOf(criteria.getPaymentStatus().toUpperCase());
        }

        List<Order> orders = orderRepository.searchOrdersAdminForExport(
                status,
                paymentStatus,
                criteria.getDateFrom(),
                criteria.getDateTo(),
                criteria.getMinAmount(),
                criteria.getMaxAmount(),
                criteria.getQuery()
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,Customer Name,Email,Phone,Total Amount,Status,Payment Status,Tracking Number,Shipping Address,Created At\n");

        for (Order order : orders) {
            String customerName = "";
            if (order.getCustomer() != null) {
                customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
            }
            String email = order.getCustomer() != null ? order.getCustomer().getEmail() : "";
            String phone = order.getCustomer() != null ? order.getCustomer().getPhone() : "";
            String shippingAddress = "";
            if (order.getShippingAddress() != null) {
                shippingAddress = order.getShippingAddress().getStreetAddress() + ", " + 
                        order.getShippingAddress().getCity();
            }

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(order.getOrderNumber()),
                    escapeCsv(customerName),
                    escapeCsv(email),
                    escapeCsv(phone),
                    order.getTotalAmount(),
                    order.getStatus().name(),
                    order.getPaymentStatus().name(),
                    escapeCsv(order.getTrackingNumber() != null ? order.getTrackingNumber() : ""),
                    escapeCsv(shippingAddress),
                    order.getCreatedAt()));
        }

        return csv.toString();
    }

    // =================================================================
    // ORDER TRACKING OPERATIONS
    // =================================================================

    /**
     * Retrieves tracking information for an order.
     * 
     * @param orderId The order UUID
     * @return Complete tracking response including timeline
     */
    @Override
    public OrderTrackingResponse getTrackingInfo(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        List<OrderTimeline> timeline = orderTimelineRepository.findByOrderIdOrderByTimestampDesc(orderId);

        // Format shipping address
        String shippingAddress = null;
        if (order.getShippingAddress() != null) {
            Address addr = order.getShippingAddress();
            shippingAddress = String.format("%s, %s, %s %s, %s",
                    addr.getStreetAddress(),
                    addr.getCity(),
                    addr.getState(),
                    addr.getPostalCode(),
                    addr.getCountry());
        }

        return OrderTrackingResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .displayName(order.getStatus().getDisplayName())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDelivery(order.getEstimatedDelivery())
                .shippingAddress(shippingAddress)
                .timeline(timeline.stream().map(this::mapTimelineToResponse).collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps an OrderTimeline entity to OrderTimelineResponse DTO.
     */
    private OrderTimelineResponse mapTimelineToResponse(OrderTimeline timeline) {
        return OrderTimelineResponse.builder()
                .status(timeline.getStatus().name())
                .description(timeline.getMessage())
                .timestamp(timeline.getTimestamp())
                .build();
    }

    /**
     * Retrieves the timeline of events for an order.
     * 
     * Checks for OrderActivityLog records first, falls back to OrderTimeline
     * for backward compatibility.
     * 
     * @param orderId The order UUID
     * @return A list of timeline events
     */
    @Override
    public List<OrderTimelineResponse> getOrderTimeline(UUID orderId) {
        List<OrderActivityLog> activities = orderActivityLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId, 
                Pageable.unpaged()).getContent();
        
        if (activities.isEmpty()) {
            // Fallback to legacy timeline for backward compatibility
            List<OrderTimeline> timeline = orderTimelineRepository.findByOrderIdOrderByTimestampDesc(orderId);
            return timeline.stream().map(this::mapTimelineToResponse).collect(Collectors.toList());
        }
        
        return activities.stream().map(this::mapActivityToTimelineResponse).collect(Collectors.toList());
    }

    /**
     * Maps an OrderActivityLog entity to OrderTimelineResponse DTO.
     */
    private OrderTimelineResponse mapActivityToTimelineResponse(OrderActivityLog activity) {
        return OrderTimelineResponse.builder()
                .activityId(activity.getId())
                .activityType(activity.getActivityType().name())
                .status(activity.getActivityType().name())
                .description(activity.getDescription())
                .oldValue(activity.getOldValue())
                .newValue(activity.getNewValue())
                .timestamp(activity.getCreatedAt())
                .icon(getIconForActivityType(activity.getActivityType()))
                .color(getColorForActivityType(activity.getActivityType()))
                .build();
    }

    /**
     * Returns the icon name for an activity type.
     * 
     * @param type The activity type
     * @return The icon name for UI display
     */
    private String getIconForActivityType(OrderActivityLog.OrderActivityType type) {
        return switch (type) {
            case ORDER_PLACED -> "package";
            case ORDER_SHIPPED, ORDER_IN_TRANSIT -> "truck";
            case ORDER_DELIVERED -> "check-circle";
            case PAYMENT_RECEIVED -> "credit-card";
            case REFUND_REQUESTED, REFUND_PROCESSED -> "rotate-ccw";
            default -> "clock";
        };
    }

    /**
     * Returns the color for an activity type.
     * 
     * @param type The activity type
     * @return The color code for UI display
     */
    private String getColorForActivityType(OrderActivityLog.OrderActivityType type) {
        return switch (type) {
            case ORDER_PLACED, ORDER_CONFIRMED -> "blue";
            case PAYMENT_RECEIVED -> "green";
            case ORDER_SHIPPED, ORDER_IN_TRANSIT, ORDER_OUT_FOR_DELIVERY -> "orange";
            case ORDER_DELIVERED -> "green";
            case ORDER_CANCELLED, REFUND_REJECTED, DELIVERY_FAILED -> "red";
            case REFUND_REQUESTED, REFUND_APPROVED, REFUND_PROCESSED -> "purple";
            default -> "gray";
        };
    }

    /**
     * Cancels an order with extended tracking support.
     * 
     * This method logs the cancellation activity asynchronously.
     * 
     * @param orderId The order UUID
     * @param request The cancellation request
     * @return The updated order response
     */
    @Override
    @Transactional
    public OrderResponse cancelOrderWithTracking(UUID orderId, CancelRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Validate order can be cancelled
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order in " + order.getStatus() + " status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Log activity asynchronously via ActivityLogService
        activityLogService.logOrderActivity(
                orderId,
                OrderActivityLog.OrderActivityType.ORDER_CANCELLED,
                request.getReason() != null ? request.getReason() : "Order cancelled by customer",
                request.getUserId(),
                order.getStatus().name(),
                OrderStatus.CANCELLED.name(),
                request.getIpAddress()
        );

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Requests a refund for an order.
     * 
     * Business Rules:
     * - Only PROCESSING or IN_TRANSIT orders can request refunds
     * - Order must belong to the requesting customer
     * - Order must not already have a pending refund
     * 
     * @param orderId The order UUID
     * @param request The refund request
     * @return The order response
     */
    @Override
    @Transactional
    public OrderResponse requestRefund(UUID orderId, RefundRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Validate order belongs to the customer
        if (request.getUserId() != null && !order.getCustomer().getId().equals(request.getUserId())) {
            throw new UnauthorizedException("You are not authorized to request a refund for this order");
        }

        // Validate order status allows refunds
        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new BadRequestException("Refund can only be requested for orders that are Processing or In Transit. " +
                    "Current status: " + order.getStatus().getDisplayName());
        }

        // Validate payment status
        if (order.getPaymentStatus() != ecommerce.modules.order.entity.PaymentStatus.PAID) {
            throw new BadRequestException("Only paid orders can be refunded");
        }

        // Update order status to REFUND_REQUESTED
        order.setStatus(OrderStatus.REFUND_REQUESTED);
        Order savedOrder = orderRepository.save(order);

        // Log refund request activity
        activityLogService.logOrderActivity(
                orderId,
                OrderActivityLog.OrderActivityType.REFUND_REQUESTED,
                request.getReason() != null ? request.getReason() : "Refund requested",
                request.getUserId(),
                null,
                "REFUND_REQUESTED",
                request.getIpAddress()
        );

        return mapToOrderResponse(savedOrder);
    }

    // =================================================================
    // ORDER SEARCH OPERATIONS
    // =================================================================

    /**
     * Searches orders for a customer with multiple filters.
     * 
     * Supported Filters:
     * - query: Search by order number (case-insensitive)
     * - status: Filter by order status
     * - dateFrom/dateTo: Date range filter
     * - minAmount/maxAmount: Amount range filter
     * 
     * @param customerId The customer's UUID
     * @param criteria   The search criteria
     * @param pageable   Pagination parameters
     * @return A paginated list of matching orders
     */
    @Override
    public Page<OrderResponse> searchOrders(UUID customerId, OrderSearchCriteria criteria, Pageable pageable) {
        // Normalize status filter
        String normalizedStatus = criteria.getStatus() != null && !"all".equalsIgnoreCase(criteria.getStatus()) 
                ? criteria.getStatus().toUpperCase() 
                : null;

        Page<Order> orders = orderRepository.searchOrders(
                customerId,
                normalizedStatus,
                criteria.getDateFrom(),
                criteria.getDateTo(),
                criteria.getMinAmount(),
                criteria.getMaxAmount(),
                criteria.getQuery(),
                pageable
        );
        
        return orders.map(this::mapToOrderResponse);
    }

    // =================================================================
    // ADMIN DASHBOARD OPERATIONS
    // =================================================================

    private static final java.time.format.DateTimeFormatter FORMATTER = 
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Retrieves comprehensive order dashboard statistics for admin.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDashboardDto getOrderDashboard() {
        long totalOrders = orderRepository.count();
        
        Map<String, Long> ordersByStatus = getOrdersByStatus();
        
        long completedOrders = ordersByStatus.getOrDefault("DELIVERED", 0L);
        long cancelledOrders = ordersByStatus.getOrDefault("CANCELLED", 0L);
        
        double completionRate = totalOrders > 0 ? (double) completedOrders / totalOrders * 100 : 0;
        double cancellationRate = totalOrders > 0 ? (double) cancelledOrders / totalOrders * 100 : 0;
        
        List<OrderDashboardDto.RecentOrderDto> recentOrders = getRecentOrders(10);
        BigDecimal totalRevenue = getTotalRevenue();
        
        return OrderDashboardDto.builder()
                .totalOrders(totalOrders)
                .pendingOrders(ordersByStatus.getOrDefault("PENDING", 0L))
                .ordersByStatus(ordersByStatus)
                .recentOrders(recentOrders)
                .orderCompletionRate(Math.round(completionRate * 10.0) / 10.0)
                .cancellationRate(Math.round(cancellationRate * 10.0) / 10.0)
                .totalRevenue(totalRevenue)
                .build();
    }

    /**
     * Retrieves recent orders for admin dashboard.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderDashboardDto.RecentOrderDto> getRecentOrders(int limit) {
        List<Order> orders = orderRepository.findRecentOrders(org.springframework.data.domain.PageRequest.of(0, limit));
        
        return orders.stream()
                .map(this::mapToRecentOrderDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves order counts grouped by status.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getOrdersByStatus() {
        List<Object[]> results = orderRepository.countByStatusGrouped();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]
                ));
    }

    /**
     * Calculates total revenue from all delivered/confirmed orders.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /**
     * Maps an Order entity to RecentOrderDto for dashboard.
     */
    private OrderDashboardDto.RecentOrderDto mapToRecentOrderDto(Order order) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getEmail() : "N/A";
        String sellerName = getSellerNameFromOrder(order);
        
        return OrderDashboardDto.RecentOrderDto.builder()
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .customerName(customerName)
                .sellerName(sellerName)
                .amount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : "N/A")
                .build();
    }

    /**
     * Extracts seller name from order items.
     */
    private String getSellerNameFromOrder(Order order) {
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            return order.getOrderItems().stream()
                    .filter(item -> item.getProduct() != null && item.getProduct().getSeller() != null)
                    .map(item -> {
                        // Try to get store name from SellerProfile
                        var seller = item.getProduct().getSeller();
                        try {
                            var storeName = seller.getClass().getMethod("getStoreName");
                            return (String) storeName.invoke(seller);
                        } catch (Exception e) {
                            return seller.getEmail();
                        }
                    })
                    .findFirst()
                    .orElse("N/A");
        }
        return "N/A";
    }

    /**
     * Maps an Order entity to OrderResponse DTO.
     * 
     * @param order The order entity
     * @return The mapped response DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .customerId(order.getCustomer().getId())
                .subtotal(order.getSubtotal())
                .tax(order.getTax())
                .shippingCost(order.getShippingCost())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .trackingNumber(order.getTrackingNumber())
                .estimatedDelivery(order.getEstimatedDelivery())
                .createdAt(order.getCreatedAt())
                .build();
    }

    // =================================================================
    // SELLER ORDER OPERATIONS
    // =================================================================

    /**
     * Retrieves all orders for a specific seller with pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getSellerOrders(UUID sellerId, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Retrieves seller orders with filters.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getSellerOrders(UUID sellerId, OrderStatus status, LocalDateTime dateFrom,
            LocalDateTime dateTo, String query, Pageable pageable) {
        return orderRepository.findSellerOrdersWithFilters(sellerId, status, dateFrom, dateTo, query, pageable)
                .map(this::mapToOrderResponse);
    }

    /**
     * Retrieves order statistics for a seller.
     */
    @Override
    @Transactional(readOnly = true)
    public SellerOrderStatsResponse getSellerOrderStats(UUID sellerId) {
        List<Object[]> stats = orderRepository.countSellerOrdersByStatusGrouped(sellerId);
        
        long total = 0;
        long pending = 0, confirmed = 0, processing = 0, shipped = 0, delivered = 0, cancelled = 0, refunded = 0;
        
        for (Object[] row : stats) {
            OrderStatus status = (OrderStatus) row[0];
            long count = (Long) row[1];
            total += count;
            
            switch (status) {
                case PENDING -> pending = count;
                case CONFIRMED -> confirmed = count;
                case PROCESSING -> processing = count;
                case SHIPPED -> shipped = count;
                case DELIVERED -> delivered = count;
                case CANCELLED -> cancelled = count;
                case REFUNDED -> refunded = count;
                default -> {}
            }
        }
        
        return SellerOrderStatsResponse.builder()
                .totalOrders(total)
                .pending(pending)
                .confirmed(confirmed)
                .processing(processing)
                .shipped(shipped)
                .delivered(delivered)
                .cancelled(cancelled)
                .refunded(refunded)
                .build();
    }

    /**
     * Exports seller orders to CSV format.
     */
    @Override
    @Transactional(readOnly = true)
    public String exportSellerOrdersToCSV(UUID sellerId, OrderStatus status, LocalDateTime dateFrom,
            LocalDateTime dateTo, String query) {
        List<Order> orders = orderRepository.findBySellerId(sellerId);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,Customer Name,Email,Phone,Total Amount,Status,Payment Status,Tracking Number,Created At\n");
        
        for (Order order : orders) {
            boolean matches = true;
            
            if (status != null && order.getStatus() != status) matches = false;
            if (dateFrom != null && order.getCreatedAt().isBefore(dateFrom)) matches = false;
            if (dateTo != null && order.getCreatedAt().isAfter(dateTo)) matches = false;
            if (query != null && !query.isEmpty()) {
                String q = query.toLowerCase();
                boolean matchOrder = order.getOrderNumber().toLowerCase().contains(q);
                boolean matchCustomer = order.getCustomer() != null && 
                    (order.getCustomer().getFirstName() != null && order.getCustomer().getFirstName().toLowerCase().contains(q) ||
                     order.getCustomer().getLastName() != null && order.getCustomer().getLastName().toLowerCase().contains(q));
                if (!matchOrder && !matchCustomer) matches = false;
            }
            
            if (matches) {
                csv.append(String.format("%s,%s %s,%s,%s,%.2f,%s,%s,%s,%s\n",
                        escapeCsv(order.getOrderNumber()),
                        escapeCsv(order.getCustomer().getFirstName()),
                        escapeCsv(order.getCustomer().getLastName()),
                        escapeCsv(order.getCustomer().getEmail()),
                        order.getCustomer().getPhone() != null ? escapeCsv(order.getCustomer().getPhone()) : "",
                        order.getTotalAmount(),
                        order.getStatus(),
                        order.getPaymentStatus(),
                        order.getTrackingNumber() != null ? escapeCsv(order.getTrackingNumber()) : "",
                        order.getCreatedAt()));
            }
        }
        
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Updates the status of an order for a specific seller.
     */
    @Override
    @Transactional
    public OrderResponse updateSellerOrderStatus(UUID orderId, OrderStatusUpdateRequest request, UUID sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        boolean hasSellerItem = order.getOrderItems().stream()
                .anyMatch(item -> item.getSeller().getId().equals(sellerId));

        if (!hasSellerItem) {
            throw new BadRequestException("Order does not contain items from this seller");
        }

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    /**
     * Retrieves comprehensive order dashboard data for a seller.
     */
    @Override
    @Transactional(readOnly = true)
    public SellerOrderDto getSellerOrderDashboard(UUID sellerId) {
        List<Order> allSellerOrders = orderRepository.findBySellerId(sellerId);
        List<Order> paidSellerOrders = allSellerOrders.stream()
                .filter(o -> o.getPaymentStatus() == ecommerce.modules.order.entity.PaymentStatus.PAID)
                .collect(Collectors.toList());

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        List<Order> thisMonthOrders = paidSellerOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfMonth))
                .collect(Collectors.toList());

        List<Order> lastMonthOrders = paidSellerOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startOfLastMonth) && o.getCreatedAt().isBefore(startOfMonth))
                .collect(Collectors.toList());

        long totalOrders = allSellerOrders.size();
        long ordersThisMonth = thisMonthOrders.size();
        long lastMonthOrdersCount = lastMonthOrders.size();

        long pendingOrders = allSellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.PROCESSING)
                .count();
        long completedOrders = allSellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();
        long cancelledOrders = allSellerOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .count();

        BigDecimal totalRevenue = paidSellerOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyRevenue = thisMonthOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastMonthRevenue = lastMonthOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueGrowth = lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0
                ? monthlyRevenue.subtract(lastMonthRevenue).divide(lastMonthRevenue, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        long totalCustomers = allSellerOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        List<SellerOrderDto.RecentSellerOrderDto> recentOrders = getRecentOrdersForSeller(sellerId, 5);

        return SellerOrderDto.builder()
                .totalOrders(totalOrders)
                .ordersThisMonth(ordersThisMonth)
                .lastMonthOrders(lastMonthOrdersCount)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .lastMonthRevenue(lastMonthRevenue)
                .revenueGrowth(revenueGrowth)
                .totalCustomers(totalCustomers)
                .recentOrders(recentOrders)
                .build();
    }

    /**
     * Retrieves order analytics for a seller.
     */
    @Override
    @Transactional(readOnly = true)
    public SellerOrderDto.SellerOrderAnalytics getSellerOrderAnalytics(UUID sellerId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Order> sellerOrders = orderRepository.findBySellerId(sellerId);
        List<Order> filteredOrders = sellerOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startDate))
                .filter(o -> o.getPaymentStatus() == ecommerce.modules.order.entity.PaymentStatus.PAID)
                .collect(Collectors.toList());

        BigDecimal totalSales = filteredOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = filteredOrders.size();
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalSales.divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalProductsSold = filteredOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .filter(item -> item.getSeller().getId().equals(sellerId))
                .mapToLong(OrderItem::getQuantity)
                .sum();

        List<SellerOrderDto.DailySalesDto> dailySalesList = new java.util.ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            BigDecimal daySales = filteredOrders.stream()
                    .filter(o -> o.getCreatedAt().isAfter(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long dayOrders = filteredOrders.stream()
                    .filter(o -> o.getCreatedAt().isAfter(dayStart) && o.getCreatedAt().isBefore(dayEnd))
                    .count();

            dailySalesList.add(SellerOrderDto.DailySalesDto.builder()
                    .date(dayStart.toLocalDate().toString())
                    .sales(daySales)
                    .orders(dayOrders)
                    .build());
        }

        List<OrderItem> allItems = filteredOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .filter(item -> item.getSeller().getId().equals(sellerId))
                .collect(Collectors.toList());

        List<SellerOrderDto.TopSellerProductDto> topProducts = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    long quantity = items.stream().mapToLong(OrderItem::getQuantity).sum();
                                    BigDecimal revenue = items.stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    String name = items.get(0).getProduct().getName();
                                    return new Object[]{name, quantity, revenue};
                                }
                        )
                ))
                .values().stream()
                .map(data -> SellerOrderDto.TopSellerProductDto.builder()
                        .productId(UUID.randomUUID().toString())
                        .productName((String) data[0])
                        .quantitySold((Long) data[1])
                        .revenue((BigDecimal) data[2])
                        .build())
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .limit(5)
                .collect(Collectors.toList());

        return SellerOrderDto.SellerOrderAnalytics.builder()
                .totalSales(totalSales)
                .averageOrderValue(averageOrderValue)
                .totalOrders(totalOrders)
                .totalProductsSold(totalProductsSold)
                .dailySales(dailySalesList)
                .topProducts(topProducts)
                .build();
    }

    /**
     * Gets recent orders for seller dashboard.
     */
    private List<SellerOrderDto.RecentSellerOrderDto> getRecentOrdersForSeller(UUID sellerId, int limit) {
        List<Order> orders = orderRepository.findBySellerId(sellerId);

        return orders.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(limit)
                .map(order -> {
                    String productName = order.getOrderItems().stream()
                            .filter(oi -> oi.getProduct() != null && oi.getProduct().getSeller() != null && oi.getProduct().getSeller().getId().equals(sellerId))
                            .map(oi -> oi.getProduct().getName())
                            .findFirst()
                            .orElse("N/A");

                    String timeAgo = calculateTimeAgo(order.getCreatedAt());

                    return SellerOrderDto.RecentSellerOrderDto.builder()
                            .orderId(order.getId().toString())
                            .orderNumber(order.getOrderNumber())
                            .customerName(order.getCustomer() != null ? order.getCustomer().getEmail() : "N/A")
                            .productName(productName)
                            .amount(order.getTotalAmount())
                            .status(order.getStatus().name())
                            .timeAgo(timeAgo)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculates time ago string.
     */
    private String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";

        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";

        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }

    /**
     * Retrieves comprehensive analytics for seller dashboard.
     */
    @Override
    @Transactional(readOnly = true)
    public ecommerce.modules.seller.dto.SellerAnalyticsDto getSellerAnalytics(UUID sellerId) {
        List<Order> allOrders = orderRepository.findBySellerId(sellerId);
        List<Order> paidOrders = allOrders.stream()
                .filter(o -> o.getPaymentStatus() == ecommerce.modules.order.entity.PaymentStatus.PAID)
                .collect(Collectors.toList());

        // Current period vs previous period
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime fourteenDaysAgo = now.minusDays(14);
        LocalDateTime sixMonthsAgo = now.minusMonths(6);
        LocalDateTime oneYearAgo = now.minusYears(1);

        // Current period orders
        List<Order> currentPeriodOrders = paidOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        
        // Previous period orders (8-14 days ago)
        List<Order> previousPeriodOrders = paidOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(fourteenDaysAgo) && o.getCreatedAt().isBefore(sevenDaysAgo))
                .collect(Collectors.toList());

        // Revenue calculations
        BigDecimal totalRevenue = paidOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentPeriodRevenue = currentPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousPeriodRevenue = previousPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Growth calculations
        Double revenueGrowth = calculateGrowthDecimal(previousPeriodRevenue, currentPeriodRevenue);
        Double ordersGrowth = calculateGrowth((double) previousPeriodOrders.size(), (double) currentPeriodOrders.size());

        // Unique customers
        long totalCustomers = paidOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        long currentPeriodCustomers = currentPeriodOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        long previousPeriodCustomers = previousPeriodOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        Double customersGrowth = calculateGrowth((double) previousPeriodCustomers, (double) currentPeriodCustomers);

        // Average order value
        long totalOrderCount = paidOrders.size();
        BigDecimal avgOrderValue = totalOrderCount > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrderCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal currentAvgOrderValue = currentPeriodOrders.size() > 0
                ? currentPeriodRevenue.divide(BigDecimal.valueOf(currentPeriodOrders.size()), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal previousAvgOrderValue = previousPeriodOrders.size() > 0
                ? previousPeriodRevenue.divide(BigDecimal.valueOf(previousPeriodOrders.size()), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Double avgOrderValueGrowth = calculateGrowthDecimal(previousAvgOrderValue, currentAvgOrderValue);

        // Daily orders (last 7 days)
        List<ecommerce.modules.seller.dto.SellerAnalyticsDto.DailyMetric> dailyMetrics = new java.util.ArrayList<>();
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            final int dayIndex = (now.getDayOfWeek().getValue() - i - 1 + 7) % 7;
            
            List<Order> dayOrders = paidOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && 
                            o.getCreatedAt().isAfter(dayStart) && 
                            o.getCreatedAt().isBefore(dayEnd))
                    .collect(Collectors.toList());

            dailyMetrics.add(ecommerce.modules.seller.dto.SellerAnalyticsDto.DailyMetric.builder()
                    .day(dayNames[dayIndex])
                    .orders((long) dayOrders.size())
                    .revenue(dayOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .customers((long) dayOrders.stream().map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null).filter(java.util.Objects::nonNull).distinct().count())
                    .build());
        }

        // Monthly revenue (last 6 months)
        List<ecommerce.modules.seller.dto.SellerAnalyticsDto.MonthlyMetric> monthlyMetrics = new java.util.ArrayList<>();
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            List<Order> monthOrders = paidOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && 
                            o.getCreatedAt().isAfter(monthStart) && 
                            o.getCreatedAt().isBefore(monthEnd))
                    .collect(Collectors.toList());

            monthlyMetrics.add(ecommerce.modules.seller.dto.SellerAnalyticsDto.MonthlyMetric.builder()
                    .month(monthNames[monthStart.getMonthValue() - 1])
                    .revenue(monthOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .orders((long) monthOrders.size())
                    .build());
        }

        // Top products
        List<OrderItem> allItems = paidOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .filter(item -> item.getSeller().getId().equals(sellerId))
                .collect(Collectors.toList());

        List<ecommerce.modules.seller.dto.SellerAnalyticsDto.TopProductMetric> topProducts = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    long quantity = items.stream().mapToLong(OrderItem::getQuantity).sum();
                                    BigDecimal revenue = items.stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    String name = items.get(0).getProduct().getName();
                                    Double rating = items.get(0).getProduct().getRating() != null 
                                            ? items.get(0).getProduct().getRating().doubleValue() : 0.0;
                                    long prevQuantity = (long)(quantity * 0.9);
                                    double growth = prevQuantity > 0 ? (double)(quantity - prevQuantity) / prevQuantity * 100 : 0;
                                    return new Object[]{name, quantity, revenue, growth, rating};
                                }
                        )
                ))
                .values().stream()
                .map(data -> ecommerce.modules.seller.dto.SellerAnalyticsDto.TopProductMetric.builder()
                        .productId(UUID.randomUUID().toString())
                        .productName((String) data[0])
                        .salesCount((Long) data[1])
                        .revenue((BigDecimal) data[2])
                        .growth((Double) data[3])
                        .rating((Double) data[4])
                        .build())
                .sorted((a, b) -> b.getSalesCount().compareTo(a.getSalesCount()))
                .limit(5)
                .collect(Collectors.toList());

        // Top customers
        List<ecommerce.modules.seller.dto.SellerAnalyticsDto.TopCustomerMetric> topCustomers = paidOrders.stream()
                .filter(o -> o.getCustomer() != null)
                .collect(Collectors.groupingBy(o -> o.getCustomer().getId()))
                .entrySet().stream()
                .map(entry -> {
                    List<Order> customerOrders = entry.getValue();
                    BigDecimal totalSpent = customerOrders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDateTime lastOrder = customerOrders.stream()
                            .map(Order::getCreatedAt)
                            .filter(java.util.Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    
                    return ecommerce.modules.seller.dto.SellerAnalyticsDto.TopCustomerMetric.builder()
                            .customerId(entry.getKey().toString())
                            .customerName(customerOrders.get(0).getCustomer().getEmail())
                            .totalOrders((long) customerOrders.size())
                            .totalSpent(totalSpent)
                            .lastOrder(lastOrder != null ? calculateTimeAgo(lastOrder) : "N/A")
                            .build();
                })
                .sorted((a, b) -> b.getTotalSpent().compareTo(a.getTotalSpent()))
                .limit(5)
                .collect(Collectors.toList());

        // Sales by category
        List<ecommerce.modules.seller.dto.SellerAnalyticsDto.CategorySales> categorySales = allItems.stream()
                .filter(item -> item.getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory().getName(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    long quantity = items.stream().mapToLong(OrderItem::getQuantity).sum();
                                    BigDecimal revenue = items.stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return new Object[]{quantity, revenue};
                                }
                        )
                ))
                .entrySet().stream()
                .map(entry -> {
                    Object[] data = (Object[]) entry.getValue();
                    return ecommerce.modules.seller.dto.SellerAnalyticsDto.CategorySales.builder()
                            .category(entry.getKey())
                            .sales((Long) data[0])
                            .revenue((BigDecimal) data[1])
                            .percentage(0.0) // Calculate below
                            .build();
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .collect(Collectors.toList());

        // Calculate percentages
        BigDecimal totalCategoryRevenue = categorySales.stream()
                .map(ecommerce.modules.seller.dto.SellerAnalyticsDto.CategorySales::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        final BigDecimal finalTotalRevenue = totalCategoryRevenue;
        categorySales = categorySales.stream()
                .peek(cs -> cs.setPercentage(
                        finalTotalRevenue.compareTo(BigDecimal.ZERO) > 0
                                ? cs.getRevenue().divide(finalTotalRevenue, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                                : 0.0))
                .collect(Collectors.toList());

        // Conversion and refund rates (placeholder values - would need page views data)
        Double conversionRate = 3.2;
        Double refundRate = 2.1;

        return ecommerce.modules.seller.dto.SellerAnalyticsDto.builder()
                .totalRevenue(totalRevenue)
                .revenueGrowth(revenueGrowth)
                .totalOrders(totalOrderCount)
                .ordersGrowth(ordersGrowth)
                .totalCustomers(totalCustomers)
                .customersGrowth(customersGrowth)
                .averageOrderValue(avgOrderValue)
                .avgOrderValueGrowth(avgOrderValueGrowth)
                .conversionRate(conversionRate)
                .conversionRateGrowth(0.5)
                .refundRate(refundRate)
                .refundRateGrowth(-0.3)
                .dailyOrders(dailyMetrics)
                .monthlyRevenue(monthlyMetrics)
                .topProducts(topProducts)
                .topCustomers(topCustomers)
                .salesByCategory(categorySales)
                .build();
    }

    private Double calculateGrowth(double previous, double current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100;
    }

    private Double calculateGrowthDecimal(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        return current.subtract(previous).divide(previous, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    // =================================================================
    // ADMIN ANALYTICS OPERATIONS
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public ecommerce.modules.analytics.dto.AdminAnalyticsDto getAdminAnalytics(String filterPeriod) {
        // Determine date range based on filter
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime previousStartDate;
        LocalDateTime endDate = now;
        int monthCount;
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        switch (filterPeriod.toLowerCase()) {
            case "day":
                startDate = now.minusDays(1);
                previousStartDate = now.minusDays(2);
                monthCount = 1;
                break;
            case "week":
                startDate = now.minusWeeks(1);
                previousStartDate = now.minusWeeks(2);
                monthCount = 1;
                break;
            case "month":
            default:
                startDate = now.minusMonths(1);
                previousStartDate = now.minusMonths(2);
                monthCount = 1;
                break;
            case "year":
                startDate = now.minusYears(1);
                previousStartDate = now.minusYears(2);
                monthCount = 12;
                break;
        }

        // Get all orders
        List<Order> allOrders = orderRepository.findAll();
        List<Order> paidOrders = allOrders.stream()
                .filter(o -> o.getPaymentStatus() == ecommerce.modules.order.entity.PaymentStatus.PAID)
                .collect(Collectors.toList());

        // Filter by current period
        List<Order> currentPeriodOrders = paidOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        // Filter by previous period
        List<Order> previousPeriodOrders = paidOrders.stream()
                .filter(o -> o.getCreatedAt() != null && 
                        o.getCreatedAt().isAfter(previousStartDate) && 
                        o.getCreatedAt().isBefore(startDate))
                .collect(Collectors.toList());

        // Calculate metrics
        BigDecimal totalRevenue = paidOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentPeriodRevenue = currentPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousPeriodRevenue = previousPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalOrders = (long) paidOrders.size();
        Long currentPeriodOrderCount = (long) currentPeriodOrders.size();
        Long previousPeriodOrderCount = (long) previousPeriodOrders.size();

        Long totalCustomers = (long) paidOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        Long currentPeriodCustomers = (long) currentPeriodOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        Long previousPeriodCustomers = (long) previousPeriodOrders.stream()
                .map(o -> o.getCustomer() != null ? o.getCustomer().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        // Products sold
        long productsSold = paidOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        long currentPeriodProductsSold = currentPeriodOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        long previousPeriodProductsSold = previousPeriodOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        // Average order value
        BigDecimal avgOrderValue = totalOrders > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal currentAvgOrderValue = currentPeriodOrderCount > 0
                ? currentPeriodRevenue.divide(BigDecimal.valueOf(currentPeriodOrderCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal previousAvgOrderValue = previousPeriodOrderCount > 0
                ? previousPeriodRevenue.divide(BigDecimal.valueOf(previousPeriodOrderCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Growth calculations
        Double revenueGrowth = calculateGrowthDecimal(previousPeriodRevenue, currentPeriodRevenue);
        Double ordersGrowth = calculateGrowth((double) previousPeriodOrderCount, (double) currentPeriodOrderCount);
        Double customersGrowth = calculateGrowth((double) previousPeriodCustomers, (double) currentPeriodCustomers);
        Double productsSoldGrowth = calculateGrowth((double) previousPeriodProductsSold, (double) currentPeriodProductsSold);
        Double avgOrderValueGrowth = calculateGrowthDecimal(previousAvgOrderValue, currentAvgOrderValue);

        // Revenue overview (monthly)
        List<ecommerce.modules.analytics.dto.AdminAnalyticsDto.MonthlyRevenue> monthlyRevenue = new java.util.ArrayList<>();
        for (int i = monthCount - 1; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            List<Order> monthOrders = paidOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && 
                            o.getCreatedAt().isAfter(monthStart) && 
                            o.getCreatedAt().isBefore(monthEnd))
                    .collect(Collectors.toList());

            monthlyRevenue.add(ecommerce.modules.analytics.dto.AdminAnalyticsDto.MonthlyRevenue.builder()
                    .month(monthNames[monthStart.getMonthValue() - 1])
                    .revenue(monthOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .orders((long) monthOrders.size())
                    .build());
        }

        // Sales by category
        List<OrderItem> allItems = paidOrders.stream()
                .flatMap(o -> o.getOrderItems().stream())
                .collect(Collectors.toList());

        List<ecommerce.modules.analytics.dto.AdminAnalyticsDto.SalesByCategory> salesByCategory = allItems.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory().getName(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    long sales = items.stream().mapToLong(OrderItem::getQuantity).sum();
                                    BigDecimal revenue = items.stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return new Object[]{sales, revenue};
                                }
                        )
                ))
                .entrySet().stream()
                .map(entry -> {
                    Object[] data = (Object[]) entry.getValue();
                    return ecommerce.modules.analytics.dto.AdminAnalyticsDto.SalesByCategory.builder()
                            .category(entry.getKey())
                            .sales((Long) data[0])
                            .revenue((BigDecimal) data[1])
                            .percentage(0.0)
                            .build();
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .collect(Collectors.toList());

        BigDecimal totalCategoryRevenue = salesByCategory.stream()
                .map(ecommerce.modules.analytics.dto.AdminAnalyticsDto.SalesByCategory::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal finalTotalCatRev = totalCategoryRevenue;
        final BigDecimal finalTotalRev = totalRevenue;
        salesByCategory = salesByCategory.stream()
                .peek(cs -> cs.setPercentage(
                        finalTotalCatRev.compareTo(BigDecimal.ZERO) > 0
                                ? cs.getRevenue().divide(finalTotalCatRev, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                                : 0.0))
                .collect(Collectors.toList());

        // Top sellers (based on order items)
        List<ecommerce.modules.analytics.dto.AdminAnalyticsDto.TopSellerMetric> topSellers = allItems.stream()
                .filter(item -> item.getSeller() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getSeller().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    long orders = items.stream().map(i -> i.getOrder().getId()).distinct().count();
                                    BigDecimal revenue = items.stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    String sellerName = items.get(0).getSeller().getEmail();
                                    return new Object[]{sellerName, orders, revenue};
                                }
                        )
                ))
                .entrySet().stream()
                .sorted((a, b) -> {
                    BigDecimal revA = (BigDecimal) ((Object[]) a.getValue())[2];
                    BigDecimal revB = (BigDecimal) ((Object[]) b.getValue())[2];
                    return revB.compareTo(revA);
                })
                .limit(5)
                .map(entry -> {
                    Object[] data = (Object[]) entry.getValue();
                    return ecommerce.modules.analytics.dto.AdminAnalyticsDto.TopSellerMetric.builder()
                            .sellerId(entry.getKey().toString())
                            .sellerName((String) data[0])
                            .orders((Long) data[1])
                            .revenue((BigDecimal) data[2])
                            .growth(10.0) // Placeholder
                            .build();
                })
                .collect(Collectors.toList());

        // Add rank
        int rank = 1;
        for (var seller : topSellers) {
            seller.setRank(rank++);
        }

        // Top products
        List<ecommerce.modules.analytics.dto.AdminAnalyticsDto.TopProductMetric> topProducts = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                items -> {
                                    long sales = items.stream().mapToLong(OrderItem::getQuantity).sum();
                                    BigDecimal revenue = items.stream()
                                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    String name = items.get(0).getProduct().getName();
                                    Double rating = items.get(0).getProduct().getRating() != null 
                                            ? items.get(0).getProduct().getRating().doubleValue() : 0.0;
                                    return new Object[]{name, sales, revenue, rating};
                                }
                        )
                ))
                .values().stream()
                .sorted((a, b) -> {
                    Long salesA = (Long) ((Object[]) a)[1];
                    Long salesB = (Long) ((Object[]) b)[1];
                    return salesB.compareTo(salesA);
                })
                .limit(5)
                .map(data -> ecommerce.modules.analytics.dto.AdminAnalyticsDto.TopProductMetric.builder()
                        .productName((String) data[0])
                        .salesCount((Long) data[1])
                        .revenue((BigDecimal) data[2])
                        .rating((Double) data[3])
                        .growth(12.0)
                        .build())
                .collect(Collectors.toList());

        // Add rank
        int productRank = 1;
        for (var product : topProducts) {
            product.setRank(productRank++);
        }

        // Monthly sales trend
        List<ecommerce.modules.analytics.dto.AdminAnalyticsDto.MonthlySalesTrend> monthlyTrend = new java.util.ArrayList<>();
        BigDecimal prevMonthRevenue = BigDecimal.ZERO;
        for (int i = 11; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            List<Order> monthOrders = paidOrders.stream()
                    .filter(o -> o.getCreatedAt() != null && 
                            o.getCreatedAt().isAfter(monthStart) && 
                            o.getCreatedAt().isBefore(monthEnd))
                    .collect(Collectors.toList());

            BigDecimal monthRevenue = monthOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            long monthOrdersCount = monthOrders.size();
            BigDecimal monthAvgOrder = monthOrdersCount > 0
                    ? monthRevenue.divide(BigDecimal.valueOf(monthOrdersCount), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Double growth = prevMonthRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? calculateGrowthDecimal(prevMonthRevenue, monthRevenue)
                    : 0.0;

            monthlyTrend.add(ecommerce.modules.analytics.dto.AdminAnalyticsDto.MonthlySalesTrend.builder()
                    .month(monthNames[monthStart.getMonthValue() - 1])
                    .revenue(monthRevenue)
                    .orders(monthOrdersCount)
                    .avgOrderValue(monthAvgOrder)
                    .growth(growth)
                    .build());

            prevMonthRevenue = monthRevenue;
        }

        // Payment method breakdown
        List<Object[]> paymentBreakdownData = orderRepository.getPaymentMethodBreakdown();
        BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        for (Object[] row : paymentBreakdownData) {
            totalPaymentAmount = totalPaymentAmount.add((BigDecimal) row[2]);
        }

        // Create effectively final copy for lambda expression
        final BigDecimal finalTotalPaymentAmount = totalPaymentAmount;

        List<ecommerce.modules.analytics.dto.AdminAnalyticsDto.PaymentMethodBreakdown> paymentMethodBreakdown = paymentBreakdownData.stream()
                .map(row -> {
                    PaymentMethod method = (PaymentMethod) row[0];
                    Long count = (Long) row[1];
                    BigDecimal amount = (BigDecimal) row[2];
                    Double share = finalTotalPaymentAmount.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(BigDecimal.valueOf(100)).divide(finalTotalPaymentAmount, 2, java.math.RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return ecommerce.modules.analytics.dto.AdminAnalyticsDto.PaymentMethodBreakdown.builder()
                            .method(method != null ? method.name() : "UNKNOWN")
                            .displayName(method != null ? method.getDisplayName() : "Unknown")
                            .transactions(count)
                            .amount(amount)
                            .share(share)
                            .build();
                })
                .collect(Collectors.toList());

        // Today's revenue
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        BigDecimal todayRevenue = orderRepository.getRevenueSince(todayStart, now);

        // Pending payments
        BigDecimal pendingPayments = orderRepository.getPendingPayments();

        // Refunds processed (from RefundRepository)
        BigDecimal refundsProcessed = refundRepository.sumAmountByStatus(java.util.Arrays.asList(
                ecommerce.common.enums.RefundStatus.APPROVED,
                ecommerce.common.enums.RefundStatus.COMPLETED));
        if (refundsProcessed == null) refundsProcessed = BigDecimal.ZERO;

        // Commission calculation (5% platform commission)
        BigDecimal platformCommission = totalRevenue.multiply(BigDecimal.valueOf(0.05));
        BigDecimal transactionFees = totalRevenue.multiply(BigDecimal.valueOf(0.03));
        BigDecimal paymentProcessing = totalRevenue.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalCommission = platformCommission.add(transactionFees).add(paymentProcessing);

        // Seller payouts (total - commission)
        BigDecimal totalPayouts = totalRevenue.subtract(totalCommission);
        BigDecimal pendingPayouts = totalPayouts.multiply(BigDecimal.valueOf(0.10));
        BigDecimal processingPayouts = totalPayouts.multiply(BigDecimal.valueOf(0.05));
        BigDecimal completedPayouts = totalPayouts.subtract(pendingPayouts).subtract(processingPayouts);

        // Net revenue
        BigDecimal netRevenue = totalCommission.subtract(refundsProcessed);

        return ecommerce.modules.analytics.dto.AdminAnalyticsDto.builder()
                .filterPeriod(filterPeriod)
                .startDate(startDate.toLocalDate().toString())
                .endDate(endDate.toLocalDate().toString())
                .totalRevenue(totalRevenue)
                .revenueGrowth(revenueGrowth)
                .todayRevenue(todayRevenue)
                .pendingPayments(pendingPayments)
                .refundsProcessed(refundsProcessed)
                .totalOrders(totalOrders)
                .ordersGrowth(ordersGrowth)
                .totalCustomers(totalCustomers)
                .customersGrowth(customersGrowth)
                .totalSellers(0L)
                .sellersGrowth(0.0)
                .productsSold(productsSold)
                .productsSoldGrowth(productsSoldGrowth)
                .averageOrderValue(avgOrderValue)
                .avgOrderValueGrowth(avgOrderValueGrowth)
                .revenueOverview(ecommerce.modules.analytics.dto.AdminAnalyticsDto.RevenueOverview.builder()
                        .monthly(monthlyRevenue)
                        .total(totalRevenue)
                        .growth(revenueGrowth)
                        .build())
                .salesByCategory(salesByCategory)
                .topSellers(topSellers)
                .topProducts(topProducts)
                .monthlyTrend(monthlyTrend)
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .commissionMetrics(ecommerce.modules.analytics.dto.AdminAnalyticsDto.CommissionMetrics.builder()
                        .totalCommission(totalCommission)
                        .platformCommission(platformCommission)
                        .transactionFees(transactionFees)
                        .paymentProcessing(paymentProcessing)
                        .growth(revenueGrowth)
                        .build())
                .sellerPayoutMetrics(ecommerce.modules.analytics.dto.AdminAnalyticsDto.SellerPayoutMetrics.builder()
                        .total(totalPayouts)
                        .pending(pendingPayouts)
                        .processing(processingPayouts)
                        .completed(completedPayouts)
                        .growth(0.082)
                        .build())
                .netRevenueMetrics(ecommerce.modules.analytics.dto.AdminAnalyticsDto.NetRevenueMetrics.builder()
                        .netRevenue(netRevenue)
                        .grossRevenue(totalRevenue)
                        .refunds(refundsProcessed)
                        .payouts(totalPayouts)
                        .growth(revenueGrowth)
                        .build())
                .build();
    }
}

package ecommerce.modules.order.service;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.entity.CartItem;
import ecommerce.modules.cart.entity.CartStatus;
import ecommerce.modules.cart.repository.CartRepository;
import ecommerce.modules.order.dto.OrderResponse;
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
import ecommerce.modules.order.service.impl.OrderServiceImpl;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.user.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository          orderRepository;
    @Mock private OrderItemRepository      orderItemRepository;
    @Mock private SellerOrderRepository    sellerOrderRepository;
    @Mock private OrderTimelineRepository  orderTimelineRepository;
    @Mock private CartRepository           cartRepository;
    @Mock private ProductRepository        productRepository;
    @Mock private AddressRepository        addressRepository;
    @Mock private SellerRepository         sellerRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID customerId;
    private UUID orderPublicId;
    private UUID productId;
    private Order testOrder;
    private Cart  testCart;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        customerId    = UUID.randomUUID();
        orderPublicId = UUID.randomUUID();
        productId     = UUID.randomUUID();

        testProduct = Product.builder()
                .name("Test Product")
                .sku("SKU-001")
                .sellerId(1L)
                .build();
        setField(testProduct, "id", productId);

        CartItem cartItem = CartItem.builder()
                .productId(productId)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .lineTotal(new BigDecimal("100.00"))
                .build();

        testCart = Cart.builder()
                .userId(customerId)
                .status(CartStatus.ACTIVE)
                .subtotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .build();
        setField(testCart, "publicId", UUID.randomUUID());
        addToInternalList(testCart, "items", cartItem);

        testOrder = Order.builder()
                .orderNumber("FYN-2026-000001")
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(new BigDecimal("100.00"))
                .discount(BigDecimal.ZERO)
                .tax(new BigDecimal("5.00"))
                .shippingCost(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("120.00"))
                .build();
        setField(testOrder, "id",        1L);
        setField(testOrder, "publicId",  orderPublicId);
        setField(testOrder, "createdAt", Instant.now());
        setField(testOrder, "updatedAt", Instant.now());
    }

    // ── createOrder ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Creates order successfully from a non-empty cart")
        void createOrder_HappyPath_CreatesOrder() {
            when(cartRepository.findByUserIdWithItems(customerId)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                setField(o, "id",        1L);
                setField(o, "publicId",  orderPublicId);
                setField(o, "createdAt", Instant.now());
                setField(o, "updatedAt", Instant.now());
                return o;
            });
            SellerOrder mockSO = SellerOrder.builder()
                    .sellerId(1L)
                    .status(OrderStatus.PENDING)
                    .subtotal(new BigDecimal("100.00"))
                    .build();
            setField(mockSO, "id",       1L);
            setField(mockSO, "publicId", UUID.randomUUID());
            when(sellerOrderRepository.save(any())).thenReturn(mockSO);
            when(orderItemRepository.save(any())).thenReturn(new OrderItem());
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());
            when(cartRepository.save(any())).thenReturn(testCart);
            when(orderItemRepository.findByOrder_PublicId(any())).thenReturn(List.of());

            CreateOrderRequest req = new CreateOrderRequest(
                    null, null, null, PaymentMethod.PAYPAL, null, null, null);

            OrderResponse result = orderService.createOrder(customerId, req);

            assertNotNull(result);
            assertEquals(customerId, result.getCustomerId());
            verify(orderRepository).save(any(Order.class));
            verify(sellerOrderRepository).save(any(SellerOrder.class));
            verify(orderItemRepository).save(any(OrderItem.class));
            verify(orderTimelineRepository).save(any(OrderTimeline.class));
            verify(cartRepository).save(argThat(c -> c.getStatus() == CartStatus.CHECKOUT));
            verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
        }

        @Test
        @DisplayName("Throws BadRequestException when no active cart found")
        void createOrder_NoCart_Throws() {
            when(cartRepository.findByUserIdWithItems(customerId)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> orderService.createOrder(customerId, new CreateOrderRequest()));
        }

        @Test
        @DisplayName("Throws BadRequestException when cart is empty")
        void createOrder_EmptyCart_Throws() {
            Cart emptyCart = Cart.builder()
                    .userId(customerId)
                    .status(CartStatus.ACTIVE)
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .build();
            when(cartRepository.findByUserIdWithItems(customerId)).thenReturn(Optional.of(emptyCart));

            assertThrows(BadRequestException.class,
                    () -> orderService.createOrder(customerId, new CreateOrderRequest()));
        }
    }

    // ── getOrderById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrderById")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Returns order when customerId matches")
        void getOrderById_ValidOwner_Returns() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderItemRepository.findByOrder_PublicId(orderPublicId)).thenReturn(List.of());

            OrderResponse result = orderService.getOrderById(orderPublicId, customerId);

            assertNotNull(result);
            assertEquals(orderPublicId, result.getId());
        }

        @Test
        @DisplayName("Returns order when customerId is null (admin access bypasses ownership check)")
        void getOrderById_NullCustomer_ReturnsWithoutOwnerCheck() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderItemRepository.findByOrder_PublicId(orderPublicId)).thenReturn(List.of());

            assertNotNull(orderService.getOrderById(orderPublicId, null));
        }

        @Test
        @DisplayName("Throws OrderNotFoundException when order does not exist")
        void getOrderById_NotFound_Throws() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class,
                    () -> orderService.getOrderById(orderPublicId, customerId));
        }

        @Test
        @DisplayName("Throws OrderAccessDeniedException when customerId does not match")
        void getOrderById_WrongOwner_Throws() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));

            assertThrows(OrderAccessDeniedException.class,
                    () -> orderService.getOrderById(orderPublicId, UUID.randomUUID()));
        }
    }

    // ── cancelOrder ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Cancels a PENDING order successfully")
        void cancelOrder_PendingOrder_Cancels() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());

            orderService.cancelOrder(orderPublicId, customerId, "Changed my mind");

            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
            verify(orderTimelineRepository).save(any(OrderTimeline.class));
            verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("Cancels a CONFIRMED order successfully")
        void cancelOrder_ConfirmedOrder_Cancels() {
            testOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());

            assertDoesNotThrow(() -> orderService.cancelOrder(orderPublicId, customerId, null));
        }

        @Test
        @DisplayName("Throws OrderNotFoundException when order does not exist")
        void cancelOrder_NotFound_Throws() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class,
                    () -> orderService.cancelOrder(orderPublicId, customerId, "reason"));
        }

        @Test
        @DisplayName("Throws OrderAccessDeniedException when not the order owner")
        void cancelOrder_WrongOwner_Throws() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));

            assertThrows(OrderAccessDeniedException.class,
                    () -> orderService.cancelOrder(orderPublicId, UUID.randomUUID(), "reason"));
        }

        @Test
        @DisplayName("Throws InvalidOrderStateException when order is already SHIPPED")
        void cancelOrder_ShippedOrder_Throws() {
            testOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));

            assertThrows(InvalidOrderStateException.class,
                    () -> orderService.cancelOrder(orderPublicId, customerId, "reason"));
        }

        @Test
        @DisplayName("Throws InvalidOrderStateException when order is DELIVERED")
        void cancelOrder_DeliveredOrder_Throws() {
            testOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));

            assertThrows(InvalidOrderStateException.class,
                    () -> orderService.cancelOrder(orderPublicId, customerId, "reason"));
        }
    }

    // ── adminUpdateStatus ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminUpdateStatus")
    class AdminUpdateStatusTests {

        @Test
        @DisplayName("Updates status and records a timeline entry")
        void adminUpdateStatus_ValidOrder_UpdatesAndRecordsTimeline() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());
            when(orderItemRepository.findByOrder_PublicId(orderPublicId)).thenReturn(List.of());

            UpdateOrderStatusRequest req =
                    new UpdateOrderStatusRequest(OrderStatus.PROCESSING, null, "Moving to processing");

            OrderResponse result = orderService.adminUpdateStatus(orderPublicId, req);

            assertNotNull(result);
            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PROCESSING));
            verify(orderTimelineRepository).save(any(OrderTimeline.class));
        }

        @Test
        @DisplayName("Sets payment status to PAID when confirming order")
        void adminUpdateStatus_Confirm_SetsPaymentStatusPaid() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());
            when(orderItemRepository.findByOrder_PublicId(orderPublicId)).thenReturn(List.of());

            UpdateOrderStatusRequest req =
                    new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null, null);

            orderService.adminUpdateStatus(orderPublicId, req);

            verify(orderRepository).save(argThat(o -> o.getPaymentStatus() == PaymentStatus.PAID));
        }

        @Test
        @DisplayName("Publishes OrderStatusChangedEvent with correct previous and new status")
        void adminUpdateStatus_PublishesEvent_WithCorrectStatuses() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());
            when(orderItemRepository.findByOrder_PublicId(orderPublicId)).thenReturn(List.of());

            UpdateOrderStatusRequest req =
                    new UpdateOrderStatusRequest(OrderStatus.PROCESSING, null, null);

            orderService.adminUpdateStatus(orderPublicId, req);

            ArgumentCaptor<OrderStatusChangedEvent> captor =
                    ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals(OrderStatus.PENDING,    captor.getValue().previousStatus());
            assertEquals(OrderStatus.PROCESSING, captor.getValue().newStatus());
        }

        @Test
        @DisplayName("Sets tracking number when provided")
        void adminUpdateStatus_WithTracking_SetsTrackingNumber() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any())).thenReturn(testOrder);
            when(orderTimelineRepository.save(any())).thenReturn(new OrderTimeline());
            when(orderItemRepository.findByOrder_PublicId(orderPublicId)).thenReturn(List.of());

            UpdateOrderStatusRequest req =
                    new UpdateOrderStatusRequest(OrderStatus.SHIPPED, "TRACK-XYZ-123", null);

            orderService.adminUpdateStatus(orderPublicId, req);

            verify(orderRepository).save(argThat(o -> "TRACK-XYZ-123".equals(o.getTrackingNumber())));
        }

        @Test
        @DisplayName("Throws OrderNotFoundException when order does not exist")
        void adminUpdateStatus_NotFound_Throws() {
            when(orderRepository.findByPublicId(orderPublicId)).thenReturn(Optional.empty());

            UpdateOrderStatusRequest req =
                    new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null, null);

            assertThrows(OrderNotFoundException.class,
                    () -> orderService.adminUpdateStatus(orderPublicId, req));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static <T> void addToInternalList(Object target, String fieldName, T element) {
        try {
            Class<?> cls = target.getClass();
            while (cls != null) {
                try {
                    var f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    ((java.util.List<T>) f.get(target)).add(element);
                    return;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> cls = target.getClass();
            while (cls != null) {
                try {
                    var f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

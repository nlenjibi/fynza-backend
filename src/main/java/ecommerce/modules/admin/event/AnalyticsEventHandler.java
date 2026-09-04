package ecommerce.modules.admin.event;

import ecommerce.modules.admin.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AnalyticsEventHandler
 * 
 * Async listeners for system events that trigger analytics updates.
 * Events include: Order Created/Confirmed/Cancelled, Product Added, User Registered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventHandler {

    private final AnalyticsService analyticsService;

    /**
     * Handle order created event
     */
    @Async("analyticsExecutor")
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Handling order created event for order: {}", event.orderId());
        
        analyticsService.recordEvent("ORDER_CREATED", event.orderId(), java.util.Map.of(
            "customerId", event.customerId(),
            "totalAmount", event.totalAmount(),
            "itemCount", event.itemCount()
        ));
        
        // Trigger analytics refresh
        analyticsService.refreshAnalyticsCache();
    }

    /**
     * Handle order confirmed event
     */
    @Async("analyticsExecutor")
    @EventListener
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Handling order confirmed event for order: {}", event.orderId());
        
        analyticsService.recordEvent("ORDER_CONFIRMED", event.orderId(), java.util.Map.of(
            "customerId", event.customerId(),
            "totalAmount", event.totalAmount()
        ));
    }

    /**
     * Handle order cancelled event
     */
    @Async("analyticsExecutor")
    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Handling order cancelled event for order: {}", event.orderId());
        
        analyticsService.recordEvent("ORDER_CANCELLED", event.orderId(), java.util.Map.of(
            "customerId", event.customerId(),
            "totalAmount", event.totalAmount(),
            "reason", event.reason()
        ));
    }

    /**
     * Handle product added event
     */
    @Async("analyticsExecutor")
    @EventListener
    public void handleProductAdded(ProductAddedEvent event) {
        log.info("Handling product added event for product: {}", event.productId());
        
        analyticsService.recordEvent("PRODUCT_ADDED", event.productId(), java.util.Map.of(
            "sellerId", event.sellerId(),
            "categoryId", event.categoryId(),
            "price", event.price()
        ));
    }

    /**
     * Handle user registered event
     */
    @Async("analyticsExecutor")
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Handling user registered event for user: {}", event.userId());
        
        analyticsService.recordEvent("USER_REGISTERED", event.userId(), java.util.Map.of(
            "role", event.role()
        ));
    }

    // ==================== Event Classes ====================

    /**
     * Event triggered when an order is created
     */
    public record OrderCreatedEvent(
        UUID orderId,
        UUID customerId,
        java.math.BigDecimal totalAmount,
        int itemCount
    ) {}

    /**
     * Event triggered when an order is confirmed
     */
    public record OrderConfirmedEvent(
        UUID orderId,
        UUID customerId,
        java.math.BigDecimal totalAmount
    ) {}

    /**
     * Event triggered when an order is cancelled
     */
    public record OrderCancelledEvent(
        UUID orderId,
        UUID customerId,
        java.math.BigDecimal totalAmount,
        String reason
    ) {}

    /**
     * Event triggered when a product is added
     */
    public record ProductAddedEvent(
        UUID productId,
        UUID sellerId,
        UUID categoryId,
        java.math.BigDecimal price
    ) {}

    /**
     * Event triggered when a user registers
     */
    public record UserRegisteredEvent(
        UUID userId,
        String role
    ) {}
}

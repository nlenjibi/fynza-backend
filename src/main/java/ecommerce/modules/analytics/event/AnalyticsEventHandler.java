package ecommerce.modules.analytics.event;

import ecommerce.common.event.order.OrderCancelledEvent;
import ecommerce.common.event.order.OrderPlacedEvent;
import ecommerce.common.event.payment.PaymentSucceededEvent;
import ecommerce.common.event.user.UserRegisteredEvent;
import ecommerce.modules.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Listens to domain events published by other modules and records analytics signals.
 * Runs @Async so analytics recording never blocks the caller's transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventHandler {

    private final AnalyticsService analyticsService;

    @Async("analyticsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.debug("Analytics: ORDER_PLACED {}", event.publicOrderId());
        analyticsService.recordEvent("ORDER_PLACED", event.publicOrderId(), Map.<String, Object>of(
                "customerId", String.valueOf(event.customerId()),
                "totalAmount", event.totalAmount()
        ));
        analyticsService.refreshAnalyticsCache();
    }

    @Async("analyticsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.debug("Analytics: ORDER_CANCELLED {}", event.publicOrderId());
        analyticsService.recordEvent("ORDER_CANCELLED", event.publicOrderId(), Map.<String, Object>of(
                "customerId", String.valueOf(event.customerId())
        ));
    }

    @Async("analyticsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.debug("Analytics: PAYMENT_SUCCEEDED {}", event.publicPaymentId());
        analyticsService.recordEvent("PAYMENT_SUCCEEDED", event.publicPaymentId(), Map.<String, Object>of(
                "orderId", String.valueOf(event.orderId()),
                "amount",  event.amount()
        ));
    }

    @Async("analyticsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Analytics: USER_REGISTERED {}", event.userId());
        analyticsService.recordEvent("USER_REGISTERED", event.userId(), Map.of(
                "role", event.role()
        ));
    }
}

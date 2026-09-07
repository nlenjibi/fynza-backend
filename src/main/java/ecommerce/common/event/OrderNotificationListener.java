package ecommerce.common.event;

import ecommerce.common.event.order.OrderCancelledEvent;
import ecommerce.common.event.order.OrderPlacedEvent;
import ecommerce.common.event.order.OrderShippedEvent;
import ecommerce.common.event.order.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends customer notifications for order lifecycle events.
 * All methods run AFTER_COMMIT so a failed email never rolls back the order transaction.
 * @Async offloads notification I/O from the caller's thread.
 *
 * Wire your NotificationService here once it supports the required notification types.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("[OrderNotification] Order placed — orderId={}, orderNumber={}, customer={}",
                event.orderId(), event.orderNumber(), event.customerEmail());
        // TODO: notificationService.send(ORDER_CONFIRMATION, event.customerEmail(), vars(event))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("[OrderNotification] Status changed — orderNumber={}, {} → {}, customer={}",
                event.orderNumber(), event.previousStatus(), event.newStatus(), event.customerEmail());
        // TODO: notificationService.send(ORDER_STATUS_UPDATE, event.customerEmail(), vars(event))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("[OrderNotification] Order cancelled — orderNumber={}, refundRequired={}, customer={}",
                event.orderNumber(), event.refundRequired(), event.customerEmail());
        // TODO: notificationService.send(ORDER_CANCELLED, event.customerEmail(), vars(event))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderShipped(OrderShippedEvent event) {
        log.info("[OrderNotification] Order shipped — orderNumber={}, tracking={}, customer={}",
                event.orderNumber(), event.trackingNumber(), event.customerEmail());
        // TODO: notificationService.send(ORDER_SHIPPED, event.customerEmail(), vars(event))
    }
}

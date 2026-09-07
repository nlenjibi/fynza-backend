package ecommerce.common.event;

import ecommerce.common.event.inventory.LowStockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Alerts sellers when their product stock drops below the configured threshold.
 * Runs AFTER_COMMIT — a failed alert never rolls back the sale transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLowStock(LowStockEvent event) {
        log.warn("[InventoryNotification] Low stock — product='{}', sku={}, stock={}/{}, seller={}",
                event.productName(), event.sku(), event.currentStock(),
                event.threshold(), event.sellerEmail());
        // TODO: notificationService.send(LOW_STOCK_ALERT, event.sellerEmail(), vars(event))
    }
}

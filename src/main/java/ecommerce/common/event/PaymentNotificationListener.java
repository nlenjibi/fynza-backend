package ecommerce.common.event;

import ecommerce.common.event.payment.PaymentFailedEvent;
import ecommerce.common.event.payment.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends customer notifications for payment outcomes.
 * Runs AFTER_COMMIT — a failed email never rolls back the payment record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        log.info("[PaymentNotification] Payment succeeded — orderNumber={}, amount={} {}, ref={}, customer={}",
                event.orderNumber(), event.amount(), event.currency(),
                event.providerReference(), event.customerEmail());
        // TODO: notificationService.send(PAYMENT_RECEIPT, event.customerEmail(), vars(event))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("[PaymentNotification] Payment failed — orderNumber={}, reason='{}', customer={}",
                event.orderNumber(), event.failureReason(), event.customerEmail());
        // TODO: notificationService.send(PAYMENT_FAILED, event.customerEmail(), vars(event))
    }
}

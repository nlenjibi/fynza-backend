package ecommerce.modules.notification.listener;

import ecommerce.modules.cart.event.CartAbandonedEvent;
import ecommerce.modules.notification.dto.EntityRef;
import ecommerce.modules.notification.enums.NotificationType;
import ecommerce.modules.notification.service.NotificationService;
import ecommerce.modules.order.event.OrderCancelledEvent;
import ecommerce.modules.order.event.OrderPlacedEvent;
import ecommerce.modules.order.event.OrderStatusChangedEvent;
import ecommerce.modules.payment.event.PaymentConfirmedEvent;
import ecommerce.modules.payment.event.PaymentFailedEvent;
import ecommerce.modules.product.event.ProductBackInStockEvent;
import ecommerce.modules.product.event.ProductLowStockEvent;
import ecommerce.modules.review.event.ProductReviewSubmittedEvent;
import ecommerce.modules.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.debug("[NotificationEvent] ORDER_PLACED orderId={}", event.orderId());
        notificationService.send(
            NotificationType.ORDER_PLACED,
            event.customerId(),
            event.sellerId(),
            Map.of(
                "recipientEmail", event.customerEmail(),
                "firstName", event.customerFirstName(),
                "orderNumber", event.orderNumber(),
                "totalAmount", event.totalAmount().toPlainString()
            ),
            "/orders/" + event.orderId(),
            new EntityRef("ORDER", event.orderId())
        );
        notificationService.sendBroadcast(
            NotificationType.SELLER_ORDER_RECEIVED,
            event.orderId(),
            event.sellerId(),
            Map.of(
                "orderNumber", event.orderNumber(),
                "totalAmount", event.totalAmount().toPlainString()
            )
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.debug("[NotificationEvent] ORDER_STATUS_CHANGED orderId={} status={}", event.orderId(), event.newStatus());
        NotificationType type = switch (event.newStatus()) {
            case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
            case SHIPPED   -> NotificationType.ORDER_SHIPPED;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            default        -> null;
        };
        if (type == null) return;
        Map<String, String> vars = new java.util.HashMap<>(Map.of(
            "recipientEmail", event.customerEmail(),
            "firstName", event.customerFirstName(),
            "orderNumber", event.orderNumber()
        ));
        if (event.trackingNumber() != null) vars.put("trackingNumber", event.trackingNumber());
        notificationService.send(type, event.customerId(), event.sellerId(), vars,
            "/orders/" + event.orderId(), new EntityRef("ORDER", event.orderId()));
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.debug("[NotificationEvent] ORDER_CANCELLED orderId={}", event.orderId());
        notificationService.send(
            NotificationType.ORDER_CANCELLED,
            event.customerId(),
            event.sellerId(),
            Map.of(
                "recipientEmail", event.customerEmail(),
                "firstName", event.customerFirstName(),
                "orderNumber", event.orderNumber(),
                "totalAmount", event.totalAmount().toPlainString(),
                "reason", event.cancellationReason() != null ? event.cancellationReason() : ""
            ),
            "/orders/" + event.orderId(),
            new EntityRef("ORDER", event.orderId())
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.debug("[NotificationEvent] PAYMENT_CONFIRMED orderId={}", event.orderId());
        notificationService.send(
            NotificationType.PAYMENT_CONFIRMED,
            event.customerId(),
            null,
            Map.of(
                "recipientEmail", event.customerEmail(),
                "firstName", event.customerFirstName(),
                "orderNumber", event.orderNumber(),
                "amount", event.amount().toPlainString(),
                "paymentMethod", event.paymentMethod()
            ),
            "/orders/" + event.orderId(),
            new EntityRef("ORDER", event.orderId())
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.debug("[NotificationEvent] PAYMENT_FAILED orderId={}", event.orderId());
        notificationService.send(
            NotificationType.PAYMENT_FAILED,
            event.customerId(),
            null,
            Map.of(
                "recipientEmail", event.customerEmail(),
                "firstName", event.customerFirstName(),
                "orderNumber", event.orderNumber(),
                "amount", event.amount().toPlainString(),
                "failureReason", event.failureReason() != null ? event.failureReason() : ""
            ),
            "/orders/" + event.orderId(),
            new EntityRef("ORDER", event.orderId())
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewSubmitted(ProductReviewSubmittedEvent event) {
        log.debug("[NotificationEvent] REVIEW_RECEIVED productId={}", event.productId());
        notificationService.send(
            NotificationType.REVIEW_RECEIVED,
            event.sellerId(),
            event.sellerId(),
            Map.of(
                "productName", event.productName(),
                "rating", String.valueOf(event.rating())
            ),
            "/products/" + event.productId() + "/reviews",
            new EntityRef("REVIEW", event.reviewId())
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCartAbandoned(CartAbandonedEvent event) {
        log.debug("[NotificationEvent] CART_ABANDONED customerId={}", event.customerId());
        notificationService.send(
            NotificationType.CART_ABANDONED,
            event.customerId(),
            null,
            Map.of(
                "recipientEmail", event.customerEmail(),
                "firstName", event.customerFirstName(),
                "cartValue", event.cartValue().toPlainString(),
                "itemCount", String.valueOf(event.itemCount())
            ),
            "/cart",
            new EntityRef("CART", event.cartId())
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("[NotificationEvent] USER_REGISTERED userId={}", event.userId());
        notificationService.sendToExternalRecipient(
            NotificationType.USER_EMAIL_VERIFIED,
            event.email(),
            Map.of(
                "firstName", event.firstName(),
                "lastName", event.lastName()
            )
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductLowStock(ProductLowStockEvent event) {
        log.debug("[NotificationEvent] SELLER_LOW_STOCK productId={}", event.productId());
        notificationService.send(
            NotificationType.SELLER_LOW_STOCK,
            event.sellerId(),
            event.sellerId(),
            Map.of(
                "recipientEmail", event.sellerEmail(),
                "productName", event.productName(),
                "currentStock", String.valueOf(event.currentStock())
            ),
            "/seller/products/" + event.productId(),
            new EntityRef("PRODUCT", event.productId())
        );
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductBackInStock(ProductBackInStockEvent event) {
        log.debug("[NotificationEvent] PRODUCT_BACK_IN_STOCK productId={}", event.productId());
        notificationService.sendBroadcast(
            NotificationType.PRODUCT_BACK_IN_STOCK,
            event.productId(),
            event.sellerId(),
            Map.of(
                "productName", event.productName(),
                "newStock", String.valueOf(event.newStock())
            )
        );
    }
}

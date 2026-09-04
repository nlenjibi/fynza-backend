package ecommerce.modules.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendPaymentNotification(UUID userId, String type, String message, String transactionId) {
        log.info("Sending payment notification to user: {}, type: {}", userId, type);
        
        Map<String, Object> notification = Map.of(
            "type", type,
            "message", message,
            "transactionId", transactionId != null ? transactionId : "",
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSendToUser(
            userId.toString(), 
            "/queue/payments", 
            notification
        );
        
        log.debug("Payment notification sent successfully to user: {}", userId);
    }

    public void sendCardExpiryWarning(UUID userId, String cardLast4, String expiry) {
        sendPaymentNotification(
            userId, 
            "CARD_EXPIRY_WARNING", 
            "Your card ending in " + cardLast4 + " expires on " + expiry, 
            null
        );
    }

    public void sendPaymentSuccess(UUID userId, String transactionId, String amount) {
        sendPaymentNotification(
            userId, 
            "PAYMENT_SUCCESS", 
            "Payment of " + amount + " successful", 
            transactionId
        );
    }

    public void sendPaymentFailed(UUID userId, String transactionId, String reason) {
        sendPaymentNotification(
            userId, 
            "PAYMENT_FAILED", 
            "Payment failed: " + reason, 
            transactionId
        );
    }

    public void sendRefundSuccess(UUID userId, String transactionId, String amount) {
        sendPaymentNotification(
            userId, 
            "REFUND_SUCCESS", 
            "Refund of " + amount + " processed successfully", 
            transactionId
        );
    }

    public void sendRefundFailed(UUID userId, String transactionId, String reason) {
        sendPaymentNotification(
            userId, 
            "REFUND_FAILED", 
            "Refund failed: " + reason, 
            transactionId
        );
    }
}

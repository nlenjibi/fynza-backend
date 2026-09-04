package ecommerce.modules.notification.service.impl;

import ecommerce.modules.notification.entity.Notification;
import ecommerce.modules.notification.repository.NotificationRepository;
import ecommerce.modules.notification.service.SellerNotificationService;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.allOf;

/**
 * SellerNotificationServiceImpl
 * 
 * Enhanced asynchronous implementation using CompletableFuture.allOf().
 * Sends Email, In-App, and Push notifications concurrently.
 * Supports specialized notifications for low stock, cancellations, and status changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerNotificationServiceImpl implements SellerNotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Boolean> notifySellerOfOrder(UUID sellerId, UUID orderId,
            String orderNumber, String productName, Integer quantity) {
        
        log.info("Notifying seller {} of new order {} for product {} (qty: {})",
                sellerId, orderNumber, productName, quantity);

        return CompletableFuture.supplyAsync(() -> {
            try {
                User seller = userRepository.findById(sellerId)
                        .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

                String notificationMessage = String.format(
                        "New order received: %s - %s (Qty: %d)",
                        orderNumber, productName, quantity);

                // Send all notification types concurrently using allOf()
                CompletableFuture<Boolean> inAppFuture = sendInAppNotification(seller, "ORDER",
                        "New Order Received", notificationMessage);
                CompletableFuture<Boolean> emailFuture = sendEmailNotification(seller, 
                        "New Order: " + orderNumber, notificationMessage);
                CompletableFuture<Boolean> pushFuture = sendPushNotification(seller,
                        "New Order: " + orderNumber, notificationMessage);

                // Wait for all notifications to complete
                allOf(inAppFuture, emailFuture, pushFuture).join();

                log.info("Seller {} notified of order {} successfully via all channels", sellerId, orderNumber);
                return true;

            } catch (Exception e) {
                log.error("Failed to notify seller {} of order {}: {}",
                        sellerId, orderNumber, e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Boolean> notifySellerOfCancellation(UUID sellerId, UUID orderId,
            String orderNumber, String productName, Integer quantity) {
        
        log.info("Notifying seller {} of cancelled order {} for product {} (qty: {})",
                sellerId, orderNumber, productName, quantity);

        return CompletableFuture.supplyAsync(() -> {
            try {
                User seller = userRepository.findById(sellerId)
                        .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

                String notificationMessage = String.format(
                        "Order cancelled: %s - %s (Qty: %d)",
                        orderNumber, productName, quantity);

                // Send all notification types concurrently
                CompletableFuture<Boolean> inAppFuture = sendInAppNotification(seller, "ORDER_CANCELLED",
                        "Order Cancelled", notificationMessage);
                CompletableFuture<Boolean> emailFuture = sendEmailNotification(seller, 
                        "Order Cancelled: " + orderNumber, notificationMessage);
                CompletableFuture<Boolean> pushFuture = sendPushNotification(seller,
                        "Order Cancelled: " + orderNumber, notificationMessage);

                allOf(inAppFuture, emailFuture, pushFuture).join();

                log.info("Seller {} notified of cancelled order {} successfully", sellerId, orderNumber);
                return true;

            } catch (Exception e) {
                log.error("Failed to notify seller {} of cancelled order {}: {}",
                        sellerId, orderNumber, e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Boolean> notifySellerOfLowStock(UUID sellerId, UUID productId,
            String productName, Integer currentStock) {
        
        log.info("Notifying seller {} of low stock for product {} (stock: {})",
                sellerId, productName, currentStock);

        return CompletableFuture.supplyAsync(() -> {
            try {
                User seller = userRepository.findById(sellerId)
                        .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

                String notificationMessage = String.format(
                        "Low stock alert: %s - Only %d units remaining",
                        productName, currentStock);

                // Send urgent low stock notifications via all channels
                CompletableFuture<Boolean> inAppFuture = sendInAppNotification(seller, "LOW_STOCK",
                        "Low Stock Alert", notificationMessage);
                CompletableFuture<Boolean> emailFuture = sendEmailNotification(seller, 
                        "⚠️ Low Stock Alert: " + productName, notificationMessage);
                CompletableFuture<Boolean> pushFuture = sendPushNotification(seller,
                        "Low Stock Alert: " + productName, notificationMessage);

                allOf(inAppFuture, emailFuture, pushFuture).join();

                log.info("Seller {} notified of low stock for product {} successfully", sellerId, productName);
                return true;

            } catch (Exception e) {
                log.error("Failed to notify seller {} of low stock for product {}: {}",
                        sellerId, productName, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Notify seller of order status change (enhanced for status updates)
     */
    @Async("asyncExecutor")
    public CompletableFuture<Boolean> notifySellerOfStatusChange(UUID sellerId, UUID orderId,
            String orderNumber, String oldStatus, String newStatus) {
        
        log.info("Notifying seller {} of status change for order {}: {} -> {}",
                sellerId, orderNumber, oldStatus, newStatus);

        return CompletableFuture.supplyAsync(() -> {
            try {
                User seller = userRepository.findById(sellerId)
                        .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

                String notificationMessage = String.format(
                        "Order %s status changed from %s to %s",
                        orderNumber, oldStatus, newStatus);

                // Send status change notifications concurrently
                CompletableFuture<Boolean> inAppFuture = sendInAppNotification(seller, "STATUS_CHANGE",
                        "Order Status Updated", notificationMessage);
                CompletableFuture<Boolean> emailFuture = sendEmailNotification(seller, 
                        "Order Status Update: " + orderNumber, notificationMessage);

                allOf(inAppFuture, emailFuture).join();

                log.info("Seller {} notified of status change for order {} successfully", sellerId, orderNumber);
                return true;

            } catch (Exception e) {
                log.error("Failed to notify seller {} of status change for order {}: {}",
                        sellerId, orderNumber, e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Send in-app notification (saves to database)
     */
    private CompletableFuture<Boolean> sendInAppNotification(User seller, String type, 
            String title, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Notification notification = Notification.builder()
                        .user(seller)
                        .type(type)
                        .title(title)
                        .message(message)
                        .isRead(false)
                        .build();

                notificationRepository.save(notification);
                log.debug("In-app notification saved for seller {}", seller.getId());
                return true;
            } catch (Exception e) {
                log.warn("Failed to save in-app notification for seller {}: {}", seller.getId(), e.getMessage());
                return false;
            }
        });
    }

    /**
     * Send email notification (placeholder for actual email implementation)
     */
    private CompletableFuture<Boolean> sendEmailNotification(User seller, String subject, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Placeholder for actual email sending logic
                // In production, this would integrate with an email service (e.g., SendGrid, SES)
                log.debug("Sending email to seller {}: Subject='{}', Message='{}'", 
                        seller.getEmail(), subject, message);
                
                // Simulate email sending
                // emailService.send(seller.getEmail(), subject, message);
                
                return true;
            } catch (Exception e) {
                log.warn("Failed to send email to seller {}: {}", seller.getEmail(), e.getMessage());
                return false;
            }
        });
    }

    /**
     * Send push notification (placeholder for actual push implementation)
     */
    private CompletableFuture<Boolean> sendPushNotification(User seller, String title, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Placeholder for actual push notification logic
                // In production, this would integrate with FCM, APNS, or other push services
                log.debug("Sending push notification to seller {}: Title='{}', Message='{}'", 
                        seller.getId(), title, message);
                
                // Simulate push notification sending
                // pushService.send(seller.getDeviceToken(), title, message);
                
                return true;
            } catch (Exception e) {
                log.warn("Failed to send push notification to seller {}: {}", seller.getId(), e.getMessage());
                return false;
            }
        });
    }
}

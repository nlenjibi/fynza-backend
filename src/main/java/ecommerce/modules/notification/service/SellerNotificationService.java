package ecommerce.modules.notification.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SellerNotificationService Interface
 * 
 * Interface for notifying sellers about new orders in the marketplace model.
 * Supports both email and in-app notifications.
 */
public interface SellerNotificationService {

    /**
     * Notify a seller about a new order
     * @param sellerId The seller's user ID
     * @param orderId The order ID
     * @param orderNumber The order number
     * @param productName The product name
     * @param quantity The quantity ordered
     * @return CompletableFuture containing true if notification sent successfully
     */
    CompletableFuture<Boolean> notifySellerOfOrder(UUID sellerId, UUID orderId, 
            String orderNumber, String productName, Integer quantity);

    /**
     * Notify a seller about order cancellation
     * @param sellerId The seller's user ID
     * @param orderId The order ID
     * @param orderNumber The order number
     * @param productName The product name
     * @param quantity The quantity that was cancelled
     * @return CompletableFuture containing true if notification sent successfully
     */
    CompletableFuture<Boolean> notifySellerOfCancellation(UUID sellerId, UUID orderId,
            String orderNumber, String productName, Integer quantity);

    /**
     * Notify a seller about low stock warning
     * @param sellerId The seller's user ID
     * @param productId The product ID
     * @param productName The product name
     * @param currentStock The current stock level
     * @return CompletableFuture containing true if notification sent successfully
     */
    CompletableFuture<Boolean> notifySellerOfLowStock(UUID sellerId, UUID productId,
            String productName, Integer currentStock);

    /**
     * Notify a seller about order status change
     * @param sellerId The seller's user ID
     * @param orderId The order ID
     * @param orderNumber The order number
     * @param oldStatus The previous status
     * @param newStatus The new status
     * @return CompletableFuture containing true if notification sent successfully
     */
    CompletableFuture<Boolean> notifySellerOfStatusChange(UUID sellerId, UUID orderId,
            String orderNumber, String oldStatus, String newStatus);
}

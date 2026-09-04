package ecommerce.modules.product.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * InventoryService Interface
 * 
 * Provides async inventory operations for the marketplace model.
 * Handles stock validation, reservation, and restoration for seller-driven fulfillment.
 */
public interface InventoryService {

    /**
     * Check if sufficient stock is available for a product
     * @param productId The product ID
     * @param quantity The quantity to check
     * @return CompletableFuture containing true if stock is sufficient
     */
    CompletableFuture<Boolean> checkStock(UUID productId, Integer quantity);

    /**
     * Reserve stock for an order
     * @param productId The product ID
     * @param quantity The quantity to reserve
     * @return CompletableFuture containing true if reservation successful
     */
    CompletableFuture<Boolean> reserveStock(UUID productId, Integer quantity);

    /**
     * Reduce stock after order confirmation
     * @param productId The product ID
     * @param quantity The quantity to reduce
     * @return CompletableFuture containing true if reduction successful
     */
    CompletableFuture<Boolean> reduceStock(UUID productId, Integer quantity);

    /**
     * Restore stock on order cancellation
     * @param productId The product ID
     * @param quantity The quantity to restore
     * @return CompletableFuture containing true if restoration successful
     */
    CompletableFuture<Boolean> restoreStock(UUID productId, Integer quantity);

    /**
     * Check stock for multiple items in parallel
     * @param items Map of productId to quantity
     * @return CompletableFuture containing a map of productId to boolean (true if stock sufficient)
     */
    CompletableFuture<Map<UUID, Boolean>> checkStockBatch(Map<UUID, Integer> items);

    /**
     * Reserve stock for multiple items in parallel with rollback capability
     * If any reservation fails, all previously reserved items will be rolled back
     * @param items Map of productId to quantity
     * @return CompletableFuture containing true if all reservations successful
     */
    CompletableFuture<Boolean> reserveStockBatch(Map<UUID, Integer> items);
}

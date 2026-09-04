package ecommerce.modules.cart.service;

import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.product.dto.ProductResponse;

import java.util.UUID;

/**
 * Redis-based Cart Service
 * 
 * Per smart caching guidelines:
 * - Store cart in Redis (NOT database)
 * - TTL: 30 minutes (sliding expiration)
 * - O(1) operations where possible
 * - No Caffeine (user-specific data)
 */
public interface RedisCartService {
    
    /**
     * Add item to cart
     * If item exists -> increase quantity
     * If not -> add item
     * Updates TTL on every operation
     */
    CartItemResponse addItem(UUID userId, UUID productId, int quantity);
    
    /**
     * Get cart from Redis
     * If empty -> return empty response (NO DB query)
     */
    CartResponse getCart(UUID userId);
    
    /**
     * Update item quantity
     * Remove if quantity = 0
     * Updates TTL
     */
    CartItemResponse updateItemQuantity(UUID userId, UUID productId, int quantity);
    
    /**
     * Remove single item from cart
     */
    void removeItem(UUID userId, UUID productId);
    
    /**
     * Clear entire cart
     */
    void clearCart(UUID userId);
    
    /**
     * Check if product exists in cart
     */
    boolean hasProduct(UUID userId, UUID productId);
    
    /**
     * Get item count in cart
     */
    long getItemCount(UUID userId);
}

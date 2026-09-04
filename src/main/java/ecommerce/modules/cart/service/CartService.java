package ecommerce.modules.cart.service;

import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);
    CartResponse getCartById(UUID cartId, UUID userId);
    CartItemResponse addItem(UUID userId, AddToCartRequest request);
    CartItemResponse addItemByProductId(UUID userId, UUID productId, int quantity);
    CartItemResponse updateItemQuantity(UUID userId, UUID cartItemId, int quantity);
    CartItemResponse updateItemByProductId(UUID userId, UUID productId, int quantity);
    void removeItem(UUID userId, UUID cartItemId);
    void removeItemByProductId(UUID userId, UUID productId);
    CartResponse applyCoupon(UUID userId, String couponCode);
    void clearCart(UUID userId);
    CartResponse createCart(UUID userId);
    CartResponse mergeCart(UUID userId, UUID guestCartId);
}

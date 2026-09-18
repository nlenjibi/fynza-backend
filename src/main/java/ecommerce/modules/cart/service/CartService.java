package ecommerce.modules.cart.service;

import ecommerce.modules.cart.dto.*;

import java.util.UUID;

public interface CartService {
    GuestCartResponse createGuestCart();
    CartResponse getCart(UUID userId);
    CartResponse getGuestCart(String cartToken);
    CartItemResponse addItem(UUID userId, AddToCartRequest request);
    CartItemResponse addItemToGuestCart(String cartToken, AddToCartRequest request);
    CartItemResponse updateItemQuantity(UUID userId, UUID cartItemPublicId, int quantity);
    void removeItem(UUID userId, UUID cartItemPublicId);
    CartResponse applyCoupon(UUID userId, String couponCode);
    CartResponse removeCoupon(UUID userId);
    void clearCart(UUID userId);
    CartResponse mergeCart(UUID userId, String guestCartToken);
    CartResponse refreshPrices(UUID userId);
}

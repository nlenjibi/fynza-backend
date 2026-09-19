package ecommerce.modules.cart.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.cart.dto.*;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.cart.service.CartValidationService;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart Management", description = "Cart management endpoints (mutations only — reads via GraphQL)")
public class CartController {

    private final CartService           cartService;
    private final CartValidationService validationService;

    @PostMapping("/guest")
    @Operation(summary = "Create a guest cart", description = "Returns an opaque cart token for anonymous shoppers")
    public ResponseEntity<ApiResponse<GuestCartResponse>> createGuestCart() {
        GuestCartResponse response = cartService.createGuestCart();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Guest cart created", response));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('cart:write')")
    @Operation(summary = "Add item to authenticated user cart")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest request) {
        CartItemResponse item = cartService.addItem(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", item));
    }

    @PostMapping("/guest/{cartToken}/items")
    @Operation(summary = "Add item to guest cart")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItemToGuestCart(
            @PathVariable String cartToken,
            @Valid @RequestBody AddToCartRequest request) {
        CartItemResponse item = cartService.addItemToGuestCart(cartToken, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to guest cart", item));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('cart:write')")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateItemQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CartItemResponse item = cartService.updateItemQuantity(principal.getId(), itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Item quantity updated", item));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('cart:delete')")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId) {
        cartService.removeItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }

    @PostMapping("/apply-coupon")
    @PreAuthorize("hasAuthority('cart:write')")
    @Operation(summary = "Apply coupon to cart")
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyCouponRequest request) {
        CartResponse cart = cartService.applyCoupon(principal.getId(), request.getCouponCode());
        return ResponseEntity.ok(ApiResponse.success("Coupon applied", cart));
    }

    @DeleteMapping("/coupon")
    @PreAuthorize("hasAuthority('cart:write')")
    @Operation(summary = "Remove coupon from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon(
            @AuthenticationPrincipal UserPrincipal principal) {
        CartResponse cart = cartService.removeCoupon(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Coupon removed", cart));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate cart prices before checkout — always call before placing an order")
    public ResponseEntity<ApiResponse<CartValidateResponse>> validateCart(
            @Valid @RequestBody CartValidateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cart validation complete",
                validationService.validate(request)));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('cart:delete')")
    @Operation(summary = "Clear all items from cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('cart:write')")
    @Operation(summary = "Merge guest cart into authenticated user cart")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MergeCartRequest request) {
        CartResponse cart = cartService.mergeCart(principal.getId(), request.getGuestCartToken());
        return ResponseEntity.ok(ApiResponse.success("Cart merged", cart));
    }

    @PostMapping("/refresh-prices")
    @PreAuthorize("hasAuthority('cart:write')")
    @Operation(summary = "Refresh prices for all cart items from authoritative source")
    public ResponseEntity<ApiResponse<CartResponse>> refreshPrices(
            @AuthenticationPrincipal UserPrincipal principal) {
        CartResponse cart = cartService.refreshPrices(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Prices refreshed", cart));
    }
}

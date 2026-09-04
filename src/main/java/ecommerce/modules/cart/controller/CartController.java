package ecommerce.modules.cart.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.dto.ApplyCouponRequest;
import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.cart.dto.ReservationResponse;
import ecommerce.modules.cart.dto.UpdateCartItemRequest;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.cart.async.StockReservationAsyncService;
import ecommerce.modules.auth.service.SecurityService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart Management", description = "Cart management for authenticated customers")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final StockReservationAsyncService stockReservationAsyncService;

    @GetMapping
    @Operation(summary = "Get user cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal

    ) {
        UUID userId = principal.getId();
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest request) {
        UUID userId = principal.getId();
        CartItemResponse item = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", item));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateItemQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        CartItemResponse item = cartService.updateItemQuantity(userId, itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Item quantity updated", item));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId) {
        UUID userId = principal.getId();
        cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }

    @PostMapping("/apply-coupon")
    @Operation(summary = "Apply coupon to cart")
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyCouponRequest request) {
        UUID userId = principal.getId();
        CartResponse cart = cartService.applyCoupon(userId, request.getCouponCode());
        return ResponseEntity.ok(ApiResponse.success("Coupon applied", cart));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }

    @PostMapping("/items/async")
    @Operation(summary = "Add item to cart with async stock reservation",
            description = "Returns immediately with pending status while stock is reserved in background")
    public ResponseEntity<ApiResponse<ReservationResponse>> addItemAsync(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest request) {
        UUID userId = principal.getId();
        CartItemResponse item = cartService.addItem(userId, request);
        
        stockReservationAsyncService.reserveStockAsync(item.getId())
                .thenAccept(result -> {
                    log.info("Stock reservation completed for cart item {}: {}",
                            item.getId(), result.getStatus());
                });
        
        ReservationResponse reservation = ReservationResponse.pending(
                item.getId(),
                30000L
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Stock reservation initiated", reservation));
    }

    @GetMapping("/reservations/{reservationId}")
    @Operation(summary = "Get reservation status",
            description = "Poll this endpoint to check the status of an async stock reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationStatus(
            @PathVariable UUID reservationId) {
        ReservationResponse reservation = stockReservationAsyncService.getReservationStatus(reservationId);
        return ResponseEntity.ok(ApiResponse.success("Reservation status retrieved", reservation));
    }

}

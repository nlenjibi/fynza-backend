package ecommerce.modules.wishlist.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.wishlist.dto.request.*;
import ecommerce.modules.wishlist.dto.response.*;
import ecommerce.modules.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/wishlists")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "Wishlist mutations (reads via GraphQL)")
public class WishlistController {

    private final WishlistService wishlistService;

    // =========================================================================
    // Wishlist CRUD
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Create a new wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> createWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWishlistRequest request) {
        WishlistResponse response = wishlistService.createWishlist(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wishlist created", response));
    }

    @PatchMapping("/{wishlistId}")
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Update a wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> updateWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID wishlistId,
            @Valid @RequestBody UpdateWishlistRequest request) {
        WishlistResponse response = wishlistService.updateWishlist(principal.getId(), wishlistId, request);
        return ResponseEntity.ok(ApiResponse.success("Wishlist updated", response));
    }

    @DeleteMapping("/{wishlistId}")
    @PreAuthorize("hasAuthority('wishlist:delete')")
    @Operation(summary = "Delete a wishlist (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID wishlistId) {
        wishlistService.deleteWishlist(principal.getId(), wishlistId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist deleted", null));
    }

    // =========================================================================
    // Item operations
    // =========================================================================

    @PostMapping("/{wishlistId}/items")
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Add item to wishlist")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID wishlistId,
            @Valid @RequestBody AddWishlistItemRequest request) {
        WishlistItemResponse response = wishlistService.addItem(principal.getId(), wishlistId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to wishlist", response));
    }

    @DeleteMapping("/{wishlistId}/items/{itemId}")
    @PreAuthorize("hasAuthority('wishlist:delete')")
    @Operation(summary = "Remove item from wishlist")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId) {
        wishlistService.removeItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from wishlist", null));
    }

    @PostMapping("/{wishlistId}/items/{itemId}/add-to-cart")
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Add wishlist item to cart (keep in wishlist)")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId) {
        wishlistService.addToCart(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", null));
    }

    @PostMapping("/{wishlistId}/items/{itemId}/move-to-cart")
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Move wishlist item to cart (removes from wishlist)")
    public ResponseEntity<ApiResponse<Void>> moveToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId) {
        wishlistService.moveToCart(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item moved to cart", null));
    }

    @PatchMapping("/{wishlistId}/items/{itemId}/notifications")
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Update notification preferences for a wishlist item")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> updateNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID itemId,
            @RequestBody NotificationPreferenceRequest request) {
        WishlistItemResponse response = wishlistService.updateNotificationPreferences(
                principal.getId(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Notification preferences updated", response));
    }

    // =========================================================================
    // Guest wishlist
    // =========================================================================

    @PostMapping("/guest")
    @Operation(summary = "Create a guest wishlist")
    public ResponseEntity<ApiResponse<GuestWishlistResponse>> createGuestWishlist() {
        GuestWishlistResponse response = wishlistService.createGuestWishlist();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Guest wishlist created", response));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('wishlist:write')")
    @Operation(summary = "Merge guest wishlist into authenticated user's default wishlist")
    public ResponseEntity<ApiResponse<Void>> mergeGuestWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MergeWishlistRequest request) {
        wishlistService.mergeGuestWishlist(principal.getId(), request.getGuestToken());
        return ResponseEntity.ok(ApiResponse.success("Guest wishlist merged", null));
    }

    // =========================================================================
    // Sharing
    // =========================================================================

    @PostMapping("/{wishlistId}/share")
    @PreAuthorize("hasAuthority('wishlist:share')")
    @Operation(summary = "Share a wishlist — returns a one-time share token")
    public ResponseEntity<ApiResponse<String>> shareWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID wishlistId) {
        String shareToken = wishlistService.shareWishlist(principal.getId(), wishlistId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist shared", shareToken));
    }

    @DeleteMapping("/{wishlistId}/share")
    @PreAuthorize("hasAuthority('wishlist:share')")
    @Operation(summary = "Unshare a wishlist")
    public ResponseEntity<ApiResponse<Void>> unshareWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID wishlistId) {
        wishlistService.unshareWishlist(principal.getId(), wishlistId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist unshared", null));
    }

    @PostMapping("/{wishlistId}/share/regenerate")
    @PreAuthorize("hasAuthority('wishlist:share')")
    @Operation(summary = "Regenerate share token for a wishlist")
    public ResponseEntity<ApiResponse<String>> regenerateShareToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID wishlistId) {
        String shareToken = wishlistService.regenerateShareToken(principal.getId(), wishlistId);
        return ResponseEntity.ok(ApiResponse.success("Share token regenerated", shareToken));
    }
}

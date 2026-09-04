package ecommerce.modules.wishlist.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.wishlist.dto.AddToWishlistRequest;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "Wishlist management endpoints for customers")
public class WishlistController {

    private final WishlistService wishlistService;
    private final SecurityService securityService;


    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get user wishlist", description = "Get all items in the user's wishlist")
    public ResponseEntity<ApiResponse<List<WishlistItemDto>>> getWishlist() {
        UUID userId = securityService.getCurrentUserId();
        List<WishlistItemDto> items = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully", items));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add item to wishlist", description = "Add a product to the user's wishlist")
    public ResponseEntity<ApiResponse<WishlistItemDto>> addToWishlist(
            @Valid @RequestBody AddToWishlistRequest request) {
        UUID userId = securityService.getCurrentUserId();
        WishlistItemDto item = wishlistService.addToWishlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to wishlist successfully", item));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove item from wishlist", description = "Remove an item from the user's wishlist by product ID")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable UUID id) {
        UUID userId = securityService.getCurrentUserId();
        wishlistService.removeFromWishlist(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Item removed from wishlist successfully", null));
    }

}

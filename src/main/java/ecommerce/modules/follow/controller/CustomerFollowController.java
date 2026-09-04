package ecommerce.modules.follow.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.follow.dto.FollowStatsResponse;
import ecommerce.modules.follow.dto.FollowedStoreResponse;
import ecommerce.modules.follow.service.FollowService;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customer/follows")
@RequiredArgsConstructor
@Tag(name = "Customer Following", description = "Customer following stores endpoints")
public class CustomerFollowController {

    private final FollowService followService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get followed stores", description = "Get list of stores the customer follows")
    public ResponseEntity<ApiResponse<Page<FollowedStoreResponse>>> getFollowedStores(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "followedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String search) {
        
        UUID customerId = UUID.fromString(principal.getId().toString());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FollowedStoreResponse> stores = followService.getFollowedStores(customerId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Followed stores retrieved successfully", stores));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Follow a store", description = "Follow a seller's store")
    public ResponseEntity<ApiResponse<Void>> followStore(
            @RequestParam UUID sellerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID customerId = UUID.fromString(principal.getId().toString());
        followService.followStore(customerId, sellerId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Now following the store", null));
    }

    @DeleteMapping("/{sellerId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Unfollow a store", description = "Unfollow a seller's store")
    public ResponseEntity<ApiResponse<Void>> unfollowStore(
            @PathVariable UUID sellerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID customerId = UUID.fromString(principal.getId().toString());
        followService.unfollowStore(customerId, sellerId);
        
        return ResponseEntity.ok(ApiResponse.success("Unfollowed the store", null));
    }

    @GetMapping("/check/{sellerId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check if following", description = "Check if customer is following a store")
    public ResponseEntity<ApiResponse<Boolean>> checkFollowing(
            @PathVariable UUID sellerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID customerId = UUID.fromString(principal.getId().toString());
        boolean isFollowing = followService.isFollowing(customerId, sellerId);
        
        return ResponseEntity.ok(ApiResponse.success("Follow status retrieved", isFollowing));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get follow stats", description = "Get following statistics for the customer")
    public ResponseEntity<ApiResponse<FollowStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID customerId = UUID.fromString(principal.getId().toString());
        FollowStatsResponse stats = followService.getCustomerFollowStats(customerId);
        
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved successfully", stats));
    }
}

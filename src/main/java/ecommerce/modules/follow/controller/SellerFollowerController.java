package ecommerce.modules.follow.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.follow.dto.FollowStatsResponse;
import ecommerce.modules.follow.dto.FollowerResponse;
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
@RequestMapping("/v1/sellers/followers")
@RequiredArgsConstructor
@Tag(name = "Seller Followers", description = "Seller follower management endpoints")
public class SellerFollowerController {

    private final FollowService followService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get followers", description = "Get list of followers for the seller's store")
    public ResponseEntity<ApiResponse<Page<FollowerResponse>>> getFollowers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "followedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String search) {
        
        UUID sellerId = UUID.fromString(principal.getId().toString());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FollowerResponse> followers = followService.getFollowers(sellerId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Followers retrieved successfully", followers));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get follower stats", description = "Get follower statistics for the seller's store")
    public ResponseEntity<ApiResponse<FollowStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID sellerId = UUID.fromString(principal.getId().toString());
        FollowStatsResponse stats = followService.getSellerFollowStats(sellerId);
        
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved successfully", stats));
    }
}

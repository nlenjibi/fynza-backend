package ecommerce.modules.review.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewStatsResponse;
import ecommerce.modules.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Reviews", description = "Seller review management")
public class SellerReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get reviews for seller products")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully",
                reviewService.getSellerReviews(principal.getId(), pageable)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get seller review stats")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getReviewStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getSellerReviewStats(principal.getId())));
    }

    @PostMapping("/{reviewId}/reply")
    @Operation(summary = "Reply to a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyToReview(
            @PathVariable UUID reviewId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Reply added successfully",
                reviewService.sellerReply(reviewId, principal.getId(), request.get("reply"))));
    }
}

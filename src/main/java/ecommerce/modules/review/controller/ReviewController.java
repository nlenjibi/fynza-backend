package ecommerce.modules.review.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.review.dto.*;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.review.spec.ReviewSpec;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "APIs for managing product reviews, ratings and analytics")
public class ReviewController {

    private final ReviewService reviewService;

    // ─── Public reads ─────────────────────────────────────────────────────────

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(@PathVariable UUID reviewId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReview(reviewId)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get all reviews with filtering")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0")         int           page,
            @RequestParam(defaultValue = "10")        int           size,
            @RequestParam(defaultValue = "createdAt") String        sortBy,
            @RequestParam(defaultValue = "DESC")      String        direction,
            @RequestParam(required = false) UUID      productId,
            @RequestParam(required = false) UUID      userId,
            @RequestParam(required = false) Integer   rating,
            @RequestParam(required = false) Integer   minRating,
            @RequestParam(required = false) Integer   maxRating,
            @RequestParam(required = false) Boolean   verifiedPurchase,
            @RequestParam(required = false) Boolean   approved,
            @RequestParam(required = false) Boolean   withImages,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));

        Specification<Review> spec = Specification.where(ReviewSpec.hasProductPublicId(productId))
                .and(ReviewSpec.hasCustomerPublicId(userId))
                .and(rating != null ? ReviewSpec.hasRating(rating) : ReviewSpec.ratingBetween(minRating, maxRating))
                .and(ReviewSpec.isVerifiedPurchase(verifiedPurchase))
                .and(ReviewSpec.isApproved(approved))
                .and(ReviewSpec.withImages(withImages))
                .and(createdAfter != null ? ReviewSpec.createdAfter(createdAfter.toInstant(ZoneOffset.UTC)) : null)
                .and(createdBefore != null ? ReviewSpec.createdBefore(createdBefore.toInstant(ZoneOffset.UTC)) : null)
                .and(ReviewSpec.isActive());

        Page<ReviewResponse> reviews = reviewService.findReviewsWithPredicate(spec, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched successfully", PaginatedResponse.from(reviews)));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Search reviews by text")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> searchReviews(
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC")      String direction,
            @RequestParam(required = false) String    searchText
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        Specification<Review> spec = Specification.where(ReviewSpec.textContains(searchText)).and(ReviewSpec.isActive());
        Page<ReviewResponse> reviews = reviewService.findReviewsWithPredicate(spec, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews search successful", PaginatedResponse.from(reviews)));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC")      String direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(reviewService.getProductReviews(productId, pageable))));
    }

    @PostMapping("/product/{productId}/filter")
    @Operation(summary = "Get filtered reviews for a product")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReviewResponse>>> getFilteredReviews(
            @PathVariable UUID productId,
            @Valid @RequestBody ReviewFilterRequest filters,
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC")      String direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(
                reviewService.getProductReviewsWithFilters(productId, filters, pageable))));
    }

    @GetMapping("/product/{productId}/stats")
    @Operation(summary = "Get product rating statistics")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getProductStats(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getProductRatingStats(productId)));
    }

    // ─── Authenticated user writes ────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a product review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required to create a review"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", reviewService.createReview(request, userId)));
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update your review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully",
                reviewService.updateReview(reviewId, request, principal.getId())));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    @Operation(summary = "Delete your review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @PutMapping("/{reviewId}/restore")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    @Operation(summary = "Restore a soft-deleted review")
    public ResponseEntity<ApiResponse<ReviewResponse>> restoreReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reviewId) {
        return ResponseEntity.ok(ApiResponse.success("Review restored successfully",
                reviewService.restoreReview(reviewId, principal.getId())));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @PostMapping("/{reviewId}/admin-response")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Add admin response to a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> addAdminResponse(
            @PathVariable @Positive UUID reviewId,
            @Valid @RequestBody AdminResponseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Admin response added successfully",
                reviewService.addAdminResponse(reviewId, request)));
    }

    @DeleteMapping("/admin/{reviewId}/admin-response")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Remove admin response from a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> removeAdminResponse(@PathVariable @Positive UUID reviewId) {
        return ResponseEntity.ok(ApiResponse.success("Admin response removed successfully",
                reviewService.removeAdminResponse(reviewId)));
    }

    @PutMapping("/admin/{reviewId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Approve a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(@PathVariable @Positive UUID reviewId) {
        return ResponseEntity.ok(ApiResponse.success("Review approved successfully", reviewService.approveReview(reviewId)));
    }

    @PutMapping("/admin/{reviewId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reject a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(
            @PathVariable @Positive UUID reviewId,
            @RequestBody RejectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review rejected successfully",
                reviewService.rejectReview(reviewId, request.getReason())));
    }

    @PostMapping("/admin/bulk-approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk approve reviews")
    public ResponseEntity<ApiResponse<Integer>> bulkApproveReviews(@RequestBody BulkReviewActionRequest request) {
        int count = reviewService.bulkApproveReviews(request.getIds());
        return ResponseEntity.ok(ApiResponse.success(count + " reviews approved successfully", count));
    }

    @PostMapping("/admin/bulk-reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk reject reviews")
    public ResponseEntity<ApiResponse<Integer>> bulkRejectReviews(@RequestBody BulkReviewActionRequest request) {
        int count = reviewService.bulkRejectReviews(request.getIds(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(count + " reviews rejected successfully", count));
    }

    @PostMapping("/admin/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Bulk delete reviews")
    public ResponseEntity<ApiResponse<Integer>> bulkDeleteReviews(@RequestBody BulkReviewActionRequest request) {
        int count = reviewService.bulkDeleteReviews(request.getIds());
        return ResponseEntity.ok(ApiResponse.success(count + " reviews deleted successfully", count));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Get admin review statistics")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getAdminReviewStats() {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAdminReviewStats()));
    }

    @PutMapping("/{reviewId}/helpful")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<Void>> markHelpful(@PathVariable UUID reviewId) {
        reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review marked as helpful", null));
    }
}

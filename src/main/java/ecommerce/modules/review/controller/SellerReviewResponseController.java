package ecommerce.modules.review.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.SellerReviewResponseRequest;
import ecommerce.modules.review.service.SellerReviewResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Seller REST controller for posting, updating, and deleting responses to reviews.
 * <p>
 * READ operations (sellerReviews query) are handled exclusively via GraphQL — see
 * {@link ecommerce.graphql.resolver.review.ReviewQueryResolver}. No {@code @GetMapping}
 * handlers belong here.
 */
@Slf4j
@RestController
@RequestMapping("v1/seller/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER')")
@Tag(name = "Seller Review Responses", description = "Seller mutations for responding to customer reviews")
public class SellerReviewResponseController {

    private final SellerReviewResponseService sellerReviewResponseService;

    @PostMapping("/{reviewId}/response")
    @Operation(summary = "Post a seller response to a customer review")
    public ResponseEntity<ApiResponse<ReviewResponse>> respondToReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody SellerReviewResponseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("respondToReview: reviewId={}, seller={}", reviewId, principal.getId());
        ReviewResponse response = sellerReviewResponseService.respondToReview(reviewId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Response posted successfully", response));
    }

    @PatchMapping("/{reviewId}/response")
    @Operation(summary = "Update an existing seller response")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateResponse(
            @PathVariable UUID reviewId,
            @Valid @RequestBody SellerReviewResponseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("updateResponse: reviewId={}, seller={}", reviewId, principal.getId());
        ReviewResponse response = sellerReviewResponseService.updateResponse(reviewId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Response updated successfully", response));
    }

    @DeleteMapping("/{reviewId}/response")
    @Operation(summary = "Delete a seller response from a review")
    public ResponseEntity<ApiResponse<Void>> deleteResponse(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("deleteResponse: reviewId={}, seller={}", reviewId, principal.getId());
        sellerReviewResponseService.deleteResponse(reviewId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Response deleted successfully", null));
    }
}

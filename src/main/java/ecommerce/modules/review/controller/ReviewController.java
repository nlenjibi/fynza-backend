package ecommerce.modules.review.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.review.dto.CreateReviewRequest;
import ecommerce.modules.review.dto.ReviewReportRequest;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewVoteRequest;
import ecommerce.modules.review.dto.ReviewVoteResponse;
import ecommerce.modules.review.dto.UpdateReviewRequest;
import ecommerce.modules.review.enums.ReviewMediaType;
import ecommerce.modules.review.service.ReviewReportService;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.review.service.ReviewVoteService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for review mutations.
 * <p>
 * READ operations (getReview, productReviews, myReviews, etc.) are handled
 * exclusively by GraphQL — see {@link ecommerce.graphql.resolver.review.ReviewQueryResolver}.
 * No {@code @GetMapping} handlers belong here.
 */
@Slf4j
@RestController
@RequestMapping("v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review lifecycle mutations (create, update, delete, vote, report, media)")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewVoteService reviewVoteService;
    private final ReviewReportService reviewReportService;

    // ─── Review CRUD ──────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a new review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("createReview: user={}", principal.getId());
        ReviewResponse response = reviewService.createReview(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", response));
    }

    @PatchMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update an existing review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("updateReview: reviewId={}, user={}", reviewId, principal.getId());
        ReviewResponse response = reviewService.updateReview(reviewId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("deleteReview: reviewId={}, user={}", reviewId, principal.getId());
        reviewService.deleteReview(reviewId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    // ─── Votes ────────────────────────────────────────────────────────────────

    @PostMapping("/{reviewId}/votes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vote on a review (helpful / not helpful)")
    public ResponseEntity<ApiResponse<ReviewVoteResponse>> voteOnReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewVoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("voteOnReview: reviewId={}, user={}, vote={}", reviewId, principal.getId(), request.getVoteType());
        ReviewVoteResponse response = reviewVoteService.vote(reviewId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Vote recorded", response));
    }

    @DeleteMapping("/{reviewId}/votes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove vote from a review")
    public ResponseEntity<ApiResponse<Void>> removeVote(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("removeVote: reviewId={}, user={}", reviewId, principal.getId());
        reviewVoteService.removeVote(reviewId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Vote removed successfully", null));
    }

    // ─── Reports ──────────────────────────────────────────────────────────────

    @PostMapping("/{reviewId}/reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Report a review for moderation")
    public ResponseEntity<ApiResponse<ReviewReportResponse>> reportReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("reportReview: reviewId={}, user={}, reason={}", reviewId, principal.getId(), request.getReason());
        ReviewReportResponse response = reviewReportService.reportReview(reviewId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review reported successfully", response));
    }

    // ─── Media ────────────────────────────────────────────────────────────────
    // Media storage and retrieval is handled by the Media module.
    // These endpoints acknowledge the intent and delegate actual storage to that module.

    @PostMapping("/{reviewId}/media")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Attach media to a review (media upload handled by Media module)")
    public ResponseEntity<ApiResponse<Void>> addMedia(
            @PathVariable UUID reviewId,
            @RequestParam String mediaReference,
            @RequestParam ReviewMediaType mediaType,
            @AuthenticationPrincipal UserPrincipal principal) {
        // TODO: Delegate to Media module when the media-attachment integration is implemented.
        // The Media module owns actual upload, transcoding, and CDN storage.
        log.info("addMedia: reviewId={}, user={}, mediaReference={}, mediaType={}", reviewId, principal.getId(), mediaReference, mediaType);
        return ResponseEntity.ok(ApiResponse.success("Media operation acknowledged", null));
    }

    @DeleteMapping("/{reviewId}/media/{mediaId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove media from a review (media deletion handled by Media module)")
    public ResponseEntity<ApiResponse<Void>> removeMedia(
            @PathVariable UUID reviewId,
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // TODO: Delegate to Media module when the media-removal integration is implemented.
        log.info("removeMedia: reviewId={}, mediaId={}, user={}", reviewId, mediaId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Media operation acknowledged", null));
    }
}

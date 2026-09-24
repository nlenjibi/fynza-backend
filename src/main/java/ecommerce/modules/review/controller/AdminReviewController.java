package ecommerce.modules.review.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.review.dto.ModerateReviewRequest;
import ecommerce.modules.review.dto.ResolveReportRequest;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.service.ReviewModerationService;
import ecommerce.modules.review.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Admin REST controller for review moderation mutations.
 * <p>
 * READ operations are handled exclusively via GraphQL — see
 * {@link ecommerce.graphql.resolver.review.ReviewQueryResolver} (adminReviews,
 * adminReviewReports queries). No {@code @GetMapping} handlers belong here.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin Reviews", description = "Admin review moderation mutations")
public class AdminReviewController {

    private final ReviewModerationService reviewModerationService;
    private final ReviewReportService reviewReportService;

    // ─── Review moderation ────────────────────────────────────────────────────

    @PostMapping("v1/admin/reviews/{reviewId}/moderate")
    @Operation(summary = "Apply a moderation action to a review (approve, reject, hide, flag, etc.)")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("moderateReview: reviewId={}, action={}, moderator={}", reviewId, request.getAction(), principal.getId());
        ReviewResponse response = reviewModerationService.moderateReview(reviewId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Review moderated successfully", response));
    }

    @PostMapping("v1/admin/reviews/{reviewId}/hide")
    @Operation(summary = "Hide a review with an optional reason")
    public ResponseEntity<ApiResponse<ReviewResponse>> hideReview(
            @PathVariable UUID reviewId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = body.get("reason");
        log.info("hideReview: reviewId={}, moderator={}", reviewId, principal.getId());
        ReviewResponse response = reviewModerationService.hideReview(reviewId, reason, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Review hidden successfully", response));
    }

    @PostMapping("v1/admin/reviews/{reviewId}/restore")
    @Operation(summary = "Restore a hidden or rejected review to published status")
    public ResponseEntity<ApiResponse<ReviewResponse>> restoreReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("restoreReview: reviewId={}, moderator={}", reviewId, principal.getId());
        ReviewResponse response = reviewModerationService.restoreReview(reviewId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Review restored successfully", response));
    }

    // ─── Report resolution ────────────────────────────────────────────────────

    @PostMapping("v1/admin/review-reports/{reportId}/resolve")
    @Operation(summary = "Resolve a flagged review report")
    public ResponseEntity<ApiResponse<ReviewReportResponse>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("resolveReport: reportId={}, resolution={}, moderator={}", reportId, request.getResolution(), principal.getId());
        ReviewReportResponse response = reviewReportService.resolveReport(reportId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Report resolved successfully", response));
    }
}

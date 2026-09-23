package ecommerce.graphql.resolver.review;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.review.dto.CreateReviewRequest;
import ecommerce.modules.review.dto.ModerateReviewRequest;
import ecommerce.modules.review.dto.ResolveReportRequest;
import ecommerce.modules.review.dto.ReviewReportRequest;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewVoteRequest;
import ecommerce.modules.review.dto.ReviewVoteResponse;
import ecommerce.modules.review.dto.SellerReviewResponseRequest;
import ecommerce.modules.review.dto.UpdateReviewRequest;
import ecommerce.modules.review.enums.ReviewModerationAction;
import ecommerce.modules.review.enums.ReviewReportReason;
import ecommerce.modules.review.enums.ReviewReportStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.service.ReviewModerationService;
import ecommerce.modules.review.service.ReviewReportService;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.review.service.ReviewVoteService;
import ecommerce.modules.review.service.SellerReviewResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL mutation resolver for the Review domain.
 * <p>
 * All mutations are authenticated. Reads are handled by
 * {@link ReviewQueryResolver} only.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ReviewMutationResolver {

    private final ReviewService reviewService;
    private final ReviewVoteService reviewVoteService;
    private final ReviewReportService reviewReportService;
    private final ReviewModerationService reviewModerationService;
    private final SellerReviewResponseService sellerReviewResponseService;

    // ─── Customer review lifecycle ─────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse createReview(
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL createReview: user={}", principal.getId());
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(input.containsKey("productId") ? UUID.fromString((String) input.get("productId")) : null)
                .variantId(input.containsKey("variantId") ? UUID.fromString((String) input.get("variantId")) : null)
                .storeId(input.containsKey("storeId") ? UUID.fromString((String) input.get("storeId")) : null)
                .orderId(UUID.fromString((String) input.get("orderId")))
                .orderItemId(UUID.fromString((String) input.get("orderItemId")))
                .rating((Integer) input.get("rating"))
                .title((String) input.get("title"))
                .body((String) input.get("body"))
                .build();
        return reviewService.createReview(request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse updateReview(
            @Argument String id,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateReview: reviewId={}, user={}", id, principal.getId());
        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .rating(input.containsKey("rating") ? (Integer) input.get("rating") : null)
                .title((String) input.get("title"))
                .body((String) input.get("body"))
                .build();
        return reviewService.updateReview(UUID.fromString(id), request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteReview(
            @Argument String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteReview: reviewId={}, user={}", id, principal.getId());
        reviewService.deleteReview(UUID.fromString(id), principal.getId());
        return true;
    }

    // ─── Votes ─────────────────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewVoteResponse voteOnReview(
            @Argument String reviewId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL voteOnReview: reviewId={}, user={}", reviewId, principal.getId());
        ReviewVoteRequest request = ReviewVoteRequest.builder()
                .voteType(ReviewVoteType.valueOf((String) input.get("voteType")))
                .build();
        return reviewVoteService.vote(UUID.fromString(reviewId), request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean removeReviewVote(
            @Argument String reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL removeReviewVote: reviewId={}, user={}", reviewId, principal.getId());
        reviewVoteService.removeVote(UUID.fromString(reviewId), principal.getId());
        return true;
    }

    // ─── Reports ───────────────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewReportResponse reportReview(
            @Argument String reviewId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL reportReview: reviewId={}, user={}", reviewId, principal.getId());
        ReviewReportRequest request = ReviewReportRequest.builder()
                .reason(ReviewReportReason.valueOf((String) input.get("reason")))
                .description((String) input.get("description"))
                .build();
        return reviewReportService.reportReview(UUID.fromString(reviewId), request, principal.getId());
    }

    // ─── Seller responses ───────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER')")
    public ReviewResponse respondToReview(
            @Argument String reviewId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL respondToReview: reviewId={}, seller={}", reviewId, principal.getId());
        SellerReviewResponseRequest request = SellerReviewResponseRequest.builder()
                .body((String) input.get("body"))
                .build();
        return sellerReviewResponseService.respondToReview(UUID.fromString(reviewId), request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER')")
    public ReviewResponse updateReviewResponse(
            @Argument String reviewId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateReviewResponse: reviewId={}, seller={}", reviewId, principal.getId());
        SellerReviewResponseRequest request = SellerReviewResponseRequest.builder()
                .body((String) input.get("body"))
                .build();
        return sellerReviewResponseService.updateResponse(UUID.fromString(reviewId), request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER')")
    public boolean deleteReviewResponse(
            @Argument String reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteReviewResponse: reviewId={}, seller={}", reviewId, principal.getId());
        sellerReviewResponseService.deleteResponse(UUID.fromString(reviewId), principal.getId());
        return true;
    }

    // ─── Admin moderation ───────────────────────────────────────────────────────

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewResponse moderateReview(
            @Argument String reviewId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL moderateReview: reviewId={}, action={}, moderator={}", reviewId, input.get("action"), principal.getId());
        ModerateReviewRequest request = ModerateReviewRequest.builder()
                .action(ReviewModerationAction.valueOf((String) input.get("action")))
                .reasonCode((String) input.get("reasonCode"))
                .notes((String) input.get("notes"))
                .build();
        return reviewModerationService.moderateReview(UUID.fromString(reviewId), request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewResponse hideReview(
            @Argument String reviewId,
            @Argument String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL hideReview: reviewId={}, moderator={}", reviewId, principal.getId());
        return reviewModerationService.hideReview(UUID.fromString(reviewId), reason, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewResponse restoreReview(
            @Argument String reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL restoreReview: reviewId={}, moderator={}", reviewId, principal.getId());
        return reviewModerationService.restoreReview(UUID.fromString(reviewId), principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewReportResponse resolveReviewReport(
            @Argument String reportId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL resolveReviewReport: reportId={}, moderator={}", reportId, principal.getId());
        ResolveReportRequest request = ResolveReportRequest.builder()
                .resolution(ReviewReportStatus.valueOf((String) input.get("resolution")))
                .notes((String) input.get("notes"))
                .build();
        return reviewReportService.resolveReport(UUID.fromString(reportId), request, principal.getId());
    }
}

package ecommerce.graphql.resolver.review;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.review.dto.ReviewEligibilityResponse;
import ecommerce.modules.review.dto.ReviewPageResponse;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewSummaryResponse;
import ecommerce.modules.review.enums.ReviewReportStatus;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewTargetType;
import ecommerce.modules.review.service.ReviewAggregationService;
import ecommerce.modules.review.service.ReviewReportService;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.graphql.input.PageInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * GraphQL query resolver for the Review domain.
 * <p>
 * This class handles ALL read operations for reviews. REST controllers in this module
 * handle mutations only — no {@code @GetMapping} belongs there.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ReviewQueryResolver {

    private final ReviewService reviewService;
    private final ReviewReportService reviewReportService;
    private final ReviewAggregationService reviewAggregationService;

    // ─── Public queries ───────────────────────────────────────────────────────

    @QueryMapping
    public ReviewResponse review(@Argument String id) {
        log.info("GQL review(id={})", id);
        return reviewService.getReview(UUID.fromString(id));
    }

    @QueryMapping
    public ReviewPageResponse productReviews(
            @Argument String productId,
            @Argument ReviewStatus status,
            @Argument PageInput pagination) {
        log.info("GQL productReviews(productId={}, status={})", productId, status);
        ReviewStatus effectiveStatus = status != null ? status : ReviewStatus.PUBLISHED;
        return reviewService.getProductReviews(UUID.fromString(productId), effectiveStatus, toPageable(pagination));
    }

    @QueryMapping
    public ReviewPageResponse storeReviews(
            @Argument String storeId,
            @Argument ReviewStatus status,
            @Argument PageInput pagination) {
        log.info("GQL storeReviews(storeId={}, status={})", storeId, status);
        ReviewStatus effectiveStatus = status != null ? status : ReviewStatus.PUBLISHED;
        return reviewService.getStoreReviews(UUID.fromString(storeId), effectiveStatus, toPageable(pagination));
    }

    @QueryMapping
    public ReviewSummaryResponse productReviewSummary(@Argument String productId) {
        log.info("GQL productReviewSummary(productId={})", productId);
        return reviewAggregationService.getAggregate(ReviewTargetType.PRODUCT, UUID.fromString(productId));
    }

    @QueryMapping
    public ReviewSummaryResponse storeReviewSummary(@Argument String storeId) {
        log.info("GQL storeReviewSummary(storeId={})", storeId);
        return reviewAggregationService.getAggregate(ReviewTargetType.STORE, UUID.fromString(storeId));
    }

    // ─── Authenticated queries ────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewPageResponse myReviews(
            @Argument PageInput pagination,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myReviews(user={})", principal.getId());
        return reviewService.getMyReviews(principal.getId(), toPageable(pagination));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewEligibilityResponse reviewEligibility(
            @Argument String orderId,
            @Argument String orderItemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL reviewEligibility(orderId={}, orderItemId={}, user={})", orderId, orderItemId, principal.getId());
        return reviewService.checkEligibility(
                UUID.fromString(orderId),
                UUID.fromString(orderItemId),
                principal.getId());
    }

    // ─── Admin queries ────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewPageResponse adminReviews(
            @Argument ReviewStatus status,
            @Argument PageInput pagination) {
        log.info("GQL adminReviews(status={})", status);
        return reviewService.getAdminReviews(status, toPageable(pagination));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ReviewReportPageResponse adminReviewReports(
            @Argument ReviewReportStatus status,
            @Argument PageInput pagination) {
        log.info("GQL adminReviewReports(status={})", status);
        Page<ReviewReportResponse> page = reviewReportService.getReports(status, toPageable(pagination));
        return toReportPageResponse(page);
    }

    // ─── Seller queries ───────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAnyRole('SELLER')")
    public ReviewPageResponse sellerReviews(
            @Argument ReviewStatus status,
            @Argument PageInput pagination,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerReviews(seller={}, status={})", principal.getId(), status);
        ReviewStatus effectiveStatus = status != null ? status : ReviewStatus.PUBLISHED;
        return reviewService.getStoreReviews(principal.getId(), effectiveStatus, toPageable(pagination));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Pageable toPageable(PageInput pagination) {
        int page = pagination != null ? pagination.getPage() : 0;
        int size = pagination != null ? pagination.getSize() : 20;
        return PageRequest.of(page, size);
    }

    private ReviewReportPageResponse toReportPageResponse(Page<ReviewReportResponse> page) {
        return ReviewReportPageResponse.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}

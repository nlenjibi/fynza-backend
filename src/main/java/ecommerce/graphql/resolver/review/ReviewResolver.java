package ecommerce.graphql.resolver.review;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.ReviewPage;
import ecommerce.graphql.input.AdminResponseInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.ReviewCreateInput;
import ecommerce.graphql.input.ReviewFilterInput;
import ecommerce.graphql.input.ReviewUpdateInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.review.dto.*;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.review.spec.ReviewSpec;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReviewResolver {

    private final ReviewService reviewService;

    // =========================================================================
    // PUBLIC QUERIES
    // =========================================================================

    @QueryMapping
    public ReviewResponse review(@Argument UUID id) {
        log.info("GQL review(id={})", id);
        return reviewService.getReview(id);
    }

    @QueryMapping
    public ReviewPage productReviews(@Argument UUID productId,
                                     @Argument PageInput pagination,
                                     @Argument ReviewFilterInput filter) {
        log.info("GQL productReviews(productId={})", productId);
        Pageable pageable = toPageable(pagination);
        if (filter != null) {
            Page<ReviewResponse> page = reviewService.getProductReviewsWithFilters(productId, toFilterRequest(filter), pageable);
            return toPage(page);
        }
        return toPage(reviewService.getProductReviews(productId, pageable));
    }

    @QueryMapping
    public ReviewSummaryResponse productRatingStats(@Argument UUID productId) {
        log.info("GQL productRatingStats(productId={})", productId);
        return reviewService.getProductRatingStats(productId);
    }

    @QueryMapping
    public List<ReviewResponse> mostHelpfulReviews(@Argument UUID productId, @Argument int limit) {
        log.info("GQL mostHelpfulReviews(productId={})", productId);
        return reviewService.getMostHelpfulReviews(productId, limit);
    }

    @QueryMapping
    public List<ReviewResponse> recentReviews(@Argument UUID productId, @Argument int limit) {
        log.info("GQL recentReviews(productId={})", productId);
        return reviewService.getRecentReviews(productId, limit);
    }

    @QueryMapping
    public ReviewPage reviewsWithImages(@Argument UUID productId, @Argument PageInput pagination) {
        log.info("GQL reviewsWithImages(productId={})", productId);
        return toPage(reviewService.getReviewsWithImages(productId, toPageable(pagination)));
    }

    @QueryMapping
    public ReviewPage verifiedReviews(@Argument UUID productId, @Argument PageInput pagination) {
        log.info("GQL verifiedReviews(productId={})", productId);
        return toPage(reviewService.getVerifiedReviews(productId, toPageable(pagination)));
    }

    @QueryMapping
    public ReviewPage reviewsByRating(@Argument UUID productId,
                                      @Argument Integer rating,
                                      @Argument PageInput pagination) {
        log.info("GQL reviewsByRating(productId={}, rating={})", productId, rating);
        return toPage(reviewService.getReviewsByRating(productId, rating, toPageable(pagination)));
    }

    // =========================================================================
    // AUTHENTICATED QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewPage myReviews(@Argument PageInput pagination,
                                @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myReviews(user={})", principal.getId());
        return toPage(reviewService.getUserReviews(principal.getId(), toPageable(pagination)));
    }

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewPage adminReviews(@Argument PageInput pagination, @Argument ReviewFilterInput filter) {
        log.info("GQL adminReviews");
        Pageable pageable = toPageable(pagination);
        Specification<Review> spec = filter != null ? buildPredicate(filter) : ReviewSpec.isActive();
        return toPage(reviewService.findReviewsWithPredicate(spec, pageable));
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewStatsResponse adminReviewStats() {
        log.info("GQL adminReviewStats");
        return reviewService.getAdminReviewStats();
    }

    // =========================================================================
    // AUTHENTICATED MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse createReview(@Argument ReviewCreateInput input,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL createReview(user={})", principal.getId());
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(input.getProductId())
                .rating(input.getRating())
                .title(input.getTitle())
                .comment(input.getComment())
                .pros(input.getPros())
                .cons(input.getCons())
                .images(input.getImages())
                .build();
        return reviewService.createReview(request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse updateReview(@Argument UUID id,
                                       @Argument ReviewUpdateInput input,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateReview(id={}, user={})", id, principal.getId());
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .rating(input.getRating())
                .title(input.getTitle())
                .comment(input.getComment())
                .pros(input.getPros())
                .cons(input.getCons())
                .build();
        return reviewService.updateReview(id, request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteReview(@Argument UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteReview(id={}, user={})", id, principal.getId());
        reviewService.deleteReview(id, principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse restoreReview(@Argument UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL restoreReview(id={}, user={})", id, principal.getId());
        return reviewService.restoreReview(id, principal.getId());
    }

    @MutationMapping
    public ReviewResponse markReviewHelpful(@Argument UUID id) {
        log.info("GQL markReviewHelpful(id={})", id);
        reviewService.markHelpful(id);
        return reviewService.getReview(id);
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponse approveReview(@Argument UUID id) {
        log.info("GQL approveReview(id={})", id);
        return reviewService.approveReview(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponse rejectReview(@Argument UUID id, @Argument String reason) {
        log.info("GQL rejectReview(id={})", id);
        return reviewService.rejectReview(id, reason);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponse addAdminResponse(@Argument UUID id, @Argument AdminResponseInput input) {
        log.info("GQL addAdminResponse(id={})", id);
        ecommerce.modules.review.dto.AdminResponseRequest request =
                ecommerce.modules.review.dto.AdminResponseRequest.builder()
                        .response(input.getResponse())
                        .build();
        return reviewService.addAdminResponse(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponse removeAdminResponse(@Argument UUID id) {
        log.info("GQL removeAdminResponse(id={})", id);
        return reviewService.removeAdminResponse(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public int bulkApproveReviews(@Argument List<UUID> ids) {
        log.info("GQL bulkApproveReviews(ids={})", ids);
        return reviewService.bulkApproveReviews(ids);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public int bulkRejectReviews(@Argument List<UUID> ids, @Argument String reason) {
        log.info("GQL bulkRejectReviews(ids={})", ids);
        return reviewService.bulkRejectReviews(ids, reason);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public int bulkDeleteReviews(@Argument List<UUID> ids) {
        log.info("GQL bulkDeleteReviews(ids={})", ids);
        return reviewService.bulkDeleteReviews(ids);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private ReviewPage toPage(Page<ReviewResponse> page) {
        return ReviewPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    private ReviewFilterRequest toFilterRequest(ReviewFilterInput input) {
        return ReviewFilterRequest.builder()
                .rating(input.getRating())
                .verifiedPurchase(input.getVerifiedPurchase())
                .approved(input.getApproved())
                .withImages(input.getWithImages())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build();
    }

    private Specification<Review> buildPredicate(ReviewFilterInput f) {
        Specification<Review> spec = Specification.where(ReviewSpec.hasProductPublicId(f.getProductId()))
                .and(ReviewSpec.hasCustomerPublicId(f.getCustomerId()))
                .and(f.getRating() != null ? ReviewSpec.hasRating(f.getRating()) : ReviewSpec.ratingBetween(f.getMinRating(), f.getMaxRating()))
                .and(ReviewSpec.isVerifiedPurchase(f.getVerifiedPurchase()))
                .and(ReviewSpec.isApproved(f.getApproved()))
                .and(ReviewSpec.withImages(f.getWithImages()))
                .and(ReviewSpec.textContains(f.getSearchText()))
                .and(f.getDateFrom() != null ? ReviewSpec.createdAfter(f.getDateFrom().toInstant(ZoneOffset.UTC)) : null)
                .and(f.getDateTo() != null ? ReviewSpec.createdBefore(f.getDateTo().toInstant(ZoneOffset.UTC)) : null)
                .and(ReviewSpec.isActive());
        if (Boolean.TRUE.equals(f.getPositiveOnly()))   spec = spec.and(ReviewSpec.ratingBetween(4, 5));
        if (Boolean.TRUE.equals(f.getNegativeOnly()))   spec = spec.and(ReviewSpec.ratingBetween(1, 2));
        if (Boolean.TRUE.equals(f.getNeedsAttention())) spec = spec.and(ReviewSpec.isApproved(false));
        return spec;
    }
}

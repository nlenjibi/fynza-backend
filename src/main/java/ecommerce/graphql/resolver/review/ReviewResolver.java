package ecommerce.graphql.resolver.review;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.ReviewPage;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.ReviewFilterInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.review.dto.ReviewFilterRequest;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewStatsResponse;
import ecommerce.modules.review.dto.ReviewSummaryResponse;
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
    // UX STATE MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse markReviewHelpful(@Argument UUID id) {
        log.info("GQL markReviewHelpful(id={})", id);
        reviewService.markHelpful(id);
        return reviewService.getReview(id);
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

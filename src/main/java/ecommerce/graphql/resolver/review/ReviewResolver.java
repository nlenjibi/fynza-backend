package ecommerce.graphql.resolver.review;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.ReviewPage;
import ecommerce.graphql.input.AdminResponseInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.ReviewCreateInput;
import ecommerce.graphql.input.ReviewFilterInput;
import ecommerce.graphql.input.ReviewUpdateInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.review.dto.*;
import ecommerce.modules.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
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
            ReviewFilterRequest filterRequest = toFilterRequest(filter);
            Page<ReviewResponse> page = reviewService.getProductReviewsWithFilters(productId, filterRequest, pageable);
            return toPage(page);
        }

        Page<ReviewResponse> page = reviewService.getProductReviews(productId, pageable);
        return toPage(page);
    }

    @QueryMapping
    public ReviewSummaryResponse productRatingStats(@Argument UUID productId) {
        log.info("GQL productRatingStats(productId={})", productId);
        return reviewService.getProductRatingStats(productId);
    }

    @QueryMapping
    public List<ReviewResponse> mostHelpfulReviews(@Argument UUID productId,
                                                     @Argument int limit) {
        log.info("GQL mostHelpfulReviews(productId={})", productId);
        return reviewService.getMostHelpfulReviews(productId, limit);
    }

    @QueryMapping
    public List<ReviewResponse> recentReviews(@Argument UUID productId,
                                                @Argument int limit) {
        log.info("GQL recentReviews(productId={})", productId);
        return reviewService.getRecentReviews(productId, limit);
    }

    @QueryMapping
    public ReviewPage reviewsWithImages(@Argument UUID productId,
                                         @Argument PageInput pagination) {
        log.info("GQL reviewsWithImages(productId={})", productId);
        Pageable pageable = toPageable(pagination);
        Page<ReviewResponse> page = reviewService.getReviewsWithImages(productId, pageable);
        return toPage(page);
    }

    @QueryMapping
    public ReviewPage verifiedReviews(@Argument UUID productId,
                                       @Argument PageInput pagination) {
        log.info("GQL verifiedReviews(productId={})", productId);
        Pageable pageable = toPageable(pagination);
        Page<ReviewResponse> page = reviewService.getVerifiedReviews(productId, pageable);
        return toPage(page);
    }

    @QueryMapping
    public ReviewPage reviewsByRating(@Argument UUID productId,
                                       @Argument Integer rating,
                                       @Argument PageInput pagination) {
        log.info("GQL reviewsByRating(productId={}, rating={})", productId, rating);
        Pageable pageable = toPageable(pagination);
        Page<ReviewResponse> page = reviewService.getReviewsByRating(productId, rating, pageable);
        return toPage(page);
    }

    // =========================================================================
    // AUTHENTICATED QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewPage myReviews(@Argument PageInput pagination,
                                 @ContextValue UUID userId) {
        log.info("GQL myReviews(user={})", userId);
        Pageable pageable = toPageable(pagination);
        Page<ReviewResponse> page = reviewService.getUserReviews(userId, pageable);
        return toPage(page);
    }

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewPage adminReviews(@Argument PageInput pagination,
                                    @Argument ReviewFilterInput filter) {
        log.info("GQL adminReviews");
        Pageable pageable = toPageable(pagination);

        if (filter != null) {
            com.querydsl.core.types.Predicate predicate = buildPredicate(filter);
            Page<ReviewResponse> page = reviewService.findReviewsWithPredicate(predicate, pageable);
            return toPage(page);
        }

        Page<ReviewResponse> page = reviewService.findReviewsWithPredicate(null, pageable);
        return toPage(page);
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
                                        @ContextValue UUID userId) {
        log.info("GQL createReview(user={})", userId);
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .productId(input.getProductId())
                .rating(input.getRating())
                .title(input.getTitle())
                .comment(input.getComment())
                .pros(input.getPros())
                .cons(input.getCons())
                .images(input.getImages())
                .build();
        return reviewService.createReview(request, userId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse updateReview(@Argument UUID id,
                                        @Argument ReviewUpdateInput input,
                                        @ContextValue UUID userId) {
        log.info("GQL updateReview(id={}, user={})", id, userId);
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .rating(input.getRating())
                .title(input.getTitle())
                .comment(input.getComment())
                .pros(input.getPros())
                .cons(input.getCons())
                .build();
        return reviewService.updateReview(id, request, userId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteReview(@Argument UUID id, @ContextValue UUID userId) {
        log.info("GQL deleteReview(id={}, user={})", id, userId);
        reviewService.deleteReview(id, userId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReviewResponse restoreReview(@Argument UUID id, @ContextValue UUID userId) {
        log.info("GQL restoreReview(id={}, user={})", id, userId);
        return reviewService.restoreReview(id, userId);
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
    public ReviewResponse addAdminResponse(@Argument UUID id,
                                            @Argument AdminResponseInput input) {
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

    private com.querydsl.core.types.Predicate buildPredicate(ReviewFilterInput f) {
        ecommerce.modules.review.entity.ReviewPredicates p =
                ecommerce.modules.review.entity.ReviewPredicates.builder()
                .withProductId(f.getProductId())
                .withUserId(f.getCustomerId())
                .withRating(f.getRating())
                .withMinRating(f.getMinRating())
                .withMaxRating(f.getMaxRating())
                .withVerifiedPurchase(f.getVerifiedPurchase())
                .withApproved(f.getApproved())
                .withImages(f.getWithImages())
                .withTextContaining(f.getSearchText())
                .withCreatedAfter(f.getDateFrom())
                .withCreatedBefore(f.getDateTo());
        if (Boolean.TRUE.equals(f.getPositiveOnly())) p.withPositiveRating();
        if (Boolean.TRUE.equals(f.getNegativeOnly())) p.withNegativeRating();
        if (Boolean.TRUE.equals(f.getNeedsAttention())) p.withNeedsAttention();
        return p.buildActiveOnly();
    }
}

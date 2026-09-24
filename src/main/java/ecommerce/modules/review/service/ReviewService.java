package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.*;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewTargetType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request, UUID customerId);

    ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, UUID customerId);

    void deleteReview(UUID reviewId, UUID customerId);

    ReviewResponse getReview(UUID reviewId);

    ReviewPageResponse getProductReviews(UUID productId, ReviewStatus status, Pageable pageable);

    ReviewPageResponse getStoreReviews(UUID storeId, ReviewStatus status, Pageable pageable);

    ReviewPageResponse getMyReviews(UUID customerId, Pageable pageable);

    ReviewPageResponse getAdminReviews(ReviewStatus status, Pageable pageable);

    ReviewSummaryResponse getReviewSummary(ReviewTargetType targetType, UUID targetId);

    ReviewEligibilityResponse checkEligibility(UUID orderId, UUID orderItemId, UUID customerId);
}

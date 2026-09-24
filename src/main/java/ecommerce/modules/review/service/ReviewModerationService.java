package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ModerateReviewRequest;
import ecommerce.modules.review.dto.ReviewResponse;

import java.util.UUID;

public interface ReviewModerationService {

    ReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request, UUID moderatorId);

    ReviewResponse hideReview(UUID reviewId, String reason, UUID moderatorId);

    ReviewResponse restoreReview(UUID reviewId, UUID moderatorId);
}

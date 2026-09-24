package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.SellerReviewResponseRequest;

import java.util.UUID;

public interface SellerReviewResponseService {

    ReviewResponse respondToReview(UUID reviewId, SellerReviewResponseRequest request, UUID sellerId);

    ReviewResponse updateResponse(UUID reviewId, SellerReviewResponseRequest request, UUID sellerId);

    ReviewResponse deleteResponse(UUID reviewId, UUID sellerId);
}

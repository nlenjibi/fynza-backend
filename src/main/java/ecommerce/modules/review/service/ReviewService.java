package ecommerce.modules.review.service;

import com.querydsl.core.types.Predicate;
import ecommerce.modules.review.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for review operations
 * Provides business logic for managing product reviews
 */
public interface ReviewService {

    // ==================== Basic CRUD Operations ====================

    ReviewResponse createReview(ReviewCreateRequest request, UUID userId);

    ReviewResponse updateReview(UUID reviewId, ReviewUpdateRequest request, UUID userId);

    void deleteReview(UUID reviewId, UUID userId);

    ReviewResponse getReview(UUID reviewId);

    ReviewResponse restoreReview(UUID reviewId, UUID userId);

    // ==================== Query Operations ====================

    Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable);

    Page<ReviewResponse> getProductReviewsWithFilters(UUID productId, ReviewFilterRequest filters, Pageable pageable);

    Page<ReviewResponse> getUserReviews(UUID userId, Pageable pageable);

    // ==================== Statistics & Analytics ====================

    ReviewSummaryResponse getProductRatingStats(UUID productId);

    ReviewStatsResponse getAdminReviewStats();

    ReviewStatsResponse getSellerReviewStats(UUID sellerId);

    // ==================== Voting Operations ====================

    void markHelpful(UUID reviewId);


    // ==================== Admin Operations ====================

    ReviewResponse approveReview(UUID reviewId);

    ReviewResponse rejectReview(UUID reviewId, String reason);

    ReviewResponse addAdminResponse(UUID reviewId, AdminResponseRequest request);

    ReviewResponse removeAdminResponse(UUID reviewId);


    int bulkApproveReviews(List<UUID> reviewIds);

    int bulkRejectReviews(List<UUID> reviewIds, String reason);

    int bulkDeleteReviews(List<UUID> reviewIds);

    // ==================== Utility Operations ====================



    /**
     * Find reviews with QueryDSL predicate for advanced filtering
     */
    Page<ReviewResponse> findReviewsWithPredicate(Predicate predicate, Pageable pageable);

    // ==================== Additional Query Operations ====================

    Page<ReviewResponse> getVerifiedReviews(UUID productId, Pageable pageable);

    Page<ReviewResponse> getReviewsByRating(UUID productId, Integer rating, Pageable pageable);

    List<ReviewResponse> getMostHelpfulReviews(UUID productId, int limit);

    List<ReviewResponse> getRecentReviews(UUID productId, int limit);

    Page<ReviewResponse> getReviewsWithImages(UUID productId, Pageable pageable);

    ReviewResponse sellerReply(UUID reviewId, UUID sellerId, String reply);
}

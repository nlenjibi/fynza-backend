package ecommerce.modules.review.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QueryDSL Predicate builder for Review entity
 * Provides type-safe, compile-time checked queries for complex review filtering
 * 
 * Usage:
 * <pre>
 * Predicate predicate = ReviewPredicates.builder()
 *     .withProductId(123L)
 *     .withRatingGreaterThanOrEqual(4)
 *     .withVerifiedPurchase(true)
 *     .build();
 * 
 * reviewRepository.findAll(predicate, pageable);
 * </pre>
 */
public class ReviewPredicates {

    private final BooleanBuilder builder;

    public ReviewPredicates() {
        this.builder = new BooleanBuilder();
    }

    /**
     * Create a new predicate builder
     */
    public static ReviewPredicates builder() {
        return new ReviewPredicates();
    }

    /**
     * Filter by product ID
     */
    public ReviewPredicates withProductId(UUID productId) {
        if (productId != null) {
            builder.and(QReview.review.product.id.eq(productId));
        }
        return this;
    }

    /**
     * Filter by user ID
     */
    public ReviewPredicates withUserId(UUID userId) {
        if (userId != null) {
            builder.and(QReview.review.customer.id.eq(userId));
        }
        return this;
    }

    /**
     * Filter by exact rating
     */
    public ReviewPredicates withRating(Integer rating) {
        if (rating != null) {
            builder.and(QReview.review.rating.eq(rating));
        }
        return this;
    }


    /**
     * Filter by verified purchase status
     */
    public ReviewPredicates withVerifiedPurchase(Boolean verified) {
        if (verified != null) {
            builder.and(QReview.review.verifiedPurchase.eq(verified));
        }
        return this;
    }

    /**
     * Filter by approval status
     */
    public ReviewPredicates withApproved(Boolean approved) {
        if (approved != null) {
            builder.and(QReview.review.approved.eq(approved));
        }
        return this;
    }

    /**
     * Filter reviews with images
     */
    public ReviewPredicates withImages(Boolean hasImages) {
        if (hasImages != null) {
            builder.and(QReview.review.hasImages.eq(hasImages));
        }
        return this;
    }


    /**
     * Filter by any text content (title or comment)
     */
    public ReviewPredicates withTextContaining(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            builder.and(QReview.review.title.containsIgnoreCase(keyword).or(QReview.review.comment.containsIgnoreCase(keyword)));
        }
        return this;
    }

    /**
     * Filter reviews created after date
     */
    public ReviewPredicates withCreatedAfter(LocalDateTime date) {
        if (date != null) {
            builder.and(QReview.review.createdAt.goe(date));
        }
        return this;
    }

    /**
     * Filter reviews created before date
     */
    public ReviewPredicates withCreatedBefore(LocalDateTime date) {
        if (date != null) {
            builder.and(QReview.review.createdAt.loe(date));
        }
        return this;
    }



    /**
     * Filter negative reviews (1-2 stars)
     */
    public ReviewPredicates withNegativeRating() {
        builder.and(QReview.review.rating.loe(2));
        return this;
    }

    /**
     * Filter positive reviews (4-5 stars)
     */
    public ReviewPredicates withPositiveRating() {
        builder.and(QReview.review.rating.goe(4));
        return this;
    }

    /**
     * Filter reviews needing attention
     */
    public ReviewPredicates withNeedsAttention() {
        builder.and(QReview.review.rating.loe(2).or(QReview.review.verifiedPurchase.isFalse()).or(QReview.review.approved.isFalse()));
        return this;
    }



    /**
     * Filter active reviews only
     */
    public ReviewPredicates withActive(Boolean active) {
        if (active != null) {
            builder.and(QReview.review.isActive.eq(active));
        } else {
            builder.and(QReview.review.isActive.isTrue());
        }
        return this;
    }

    /**
     * Complex search combining multiple criteria
     */
    public ReviewPredicates withSearch(UUID productId, UUID userId, Integer rating, Boolean verifiedPurchase, Boolean approved, Boolean withImages, String keyword) {
        if (productId != null) {
            builder.and(QReview.review.product.id.eq(productId));
        }
        if (userId != null) {
            builder.and(QReview.review.customer.id.eq(userId));
        }
        if (rating != null) {
            builder.and(QReview.review.rating.eq(rating));
        }
        if (verifiedPurchase != null) {
            builder.and(QReview.review.verifiedPurchase.eq(verifiedPurchase));
        }
        if (approved != null) {
            builder.and(QReview.review.approved.eq(approved));
        }
        if (withImages != null) {
            // Note: hasImages field doesn't exist in Review entity
            // This parameter is kept for compatibility but does nothing
        }
        if (keyword != null && !keyword.isEmpty()) {
            builder.and(QReview.review.title.containsIgnoreCase(keyword).or(QReview.review.comment.containsIgnoreCase(keyword)));
        }
        return this;
    }

    /**
     * Build the predicate
     */
    public Predicate build() {
        return builder.getValue();
    }

    /**
     * Build with default active filter
     */
    public Predicate buildActiveOnly() {
        builder.and(QReview.review.isActive.isTrue());
        return builder.getValue();
    }

    public ReviewPredicates withMinRating(Integer minRating) {
        if (minRating != null) {
            builder.and(QReview.review.rating.goe(minRating));
        }
        return this;
    }

    public ReviewPredicates withMaxRating(Integer maxRating) {
        if (maxRating != null) {
            builder.and(QReview.review.rating.loe(maxRating));
        }
        return this;
    }
}

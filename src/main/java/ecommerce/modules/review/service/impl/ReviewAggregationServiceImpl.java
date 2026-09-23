package ecommerce.modules.review.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.review.dto.ReviewSummaryResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewRatingAggregate;
import ecommerce.modules.review.enums.ReviewTargetType;
import ecommerce.modules.review.repository.ReviewRatingAggregateRepository;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.service.ReviewAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewAggregationServiceImpl implements ReviewAggregationService {

    private final ReviewRepository reviewRepository;
    private final ReviewRatingAggregateRepository reviewRatingAggregateRepository;

    // ─── updateAggregateForProduct ────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.REVIEW_STATS,
                key = "'" + "PRODUCT" + ":' + #productId")
    public void updateAggregateForProduct(UUID productId) {
        log.info("Updating rating aggregate for product={}", productId);

        List<Review> reviews = reviewRepository.findPublishedByProductId(productId);
        ReviewRatingAggregate aggregate = reviewRatingAggregateRepository
                .findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, productId)
                .orElseGet(() -> ReviewRatingAggregate.builder()
                        .targetType(ReviewTargetType.PRODUCT)
                        .targetId(productId)
                        .build());

        populateAggregate(aggregate, reviews);
        reviewRatingAggregateRepository.save(aggregate);
        log.info("Aggregate saved for product={}: count={} avg={}", productId, aggregate.getReviewCount(), aggregate.getAverageRating());
    }

    // ─── updateAggregateForStore ──────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.REVIEW_STATS,
                key = "'" + "STORE" + ":' + #storeId")
    public void updateAggregateForStore(UUID storeId) {
        log.info("Updating rating aggregate for store={}", storeId);

        List<Review> reviews = reviewRepository.findPublishedByStoreId(storeId);
        ReviewRatingAggregate aggregate = reviewRatingAggregateRepository
                .findByTargetTypeAndTargetId(ReviewTargetType.STORE, storeId)
                .orElseGet(() -> ReviewRatingAggregate.builder()
                        .targetType(ReviewTargetType.STORE)
                        .targetId(storeId)
                        .build());

        populateAggregate(aggregate, reviews);
        reviewRatingAggregateRepository.save(aggregate);
        log.info("Aggregate saved for store={}: count={} avg={}", storeId, aggregate.getReviewCount(), aggregate.getAverageRating());
    }

    // ─── getAggregate ─────────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.REVIEW_STATS,
               key = "#targetType + ':' + #targetId")
    public ReviewSummaryResponse getAggregate(ReviewTargetType targetType, UUID targetId) {
        ReviewRatingAggregate agg = reviewRatingAggregateRepository
                .findByTargetTypeAndTargetId(targetType, targetId)
                .orElse(null);

        if (agg == null) {
            return ReviewSummaryResponse.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .reviewCount(0L)
                    .verifiedCount(0L)
                    .build();
        }

        double verifiedPct = agg.getReviewCount() != null && agg.getReviewCount() > 0 && agg.getVerifiedCount() != null
                ? (double) agg.getVerifiedCount() / agg.getReviewCount() * 100.0
                : 0.0;

        return ReviewSummaryResponse.builder()
                .targetId(agg.getTargetId())
                .targetType(agg.getTargetType())
                .reviewCount(agg.getReviewCount() == null ? 0L : agg.getReviewCount().longValue())
                .averageRating(agg.getAverageRating())
                .rating1Count(agg.getRating1Count() == null ? 0L : agg.getRating1Count().longValue())
                .rating2Count(agg.getRating2Count() == null ? 0L : agg.getRating2Count().longValue())
                .rating3Count(agg.getRating3Count() == null ? 0L : agg.getRating3Count().longValue())
                .rating4Count(agg.getRating4Count() == null ? 0L : agg.getRating4Count().longValue())
                .rating5Count(agg.getRating5Count() == null ? 0L : agg.getRating5Count().longValue())
                .verifiedCount(agg.getVerifiedCount() == null ? 0L : agg.getVerifiedCount().longValue())
                .verifiedPercentage(verifiedPct)
                .build();
    }

    // ─── rebuildAllAggregates ─────────────────────────────────────────────────

    @Override
    public void rebuildAllAggregates() {
        log.info("Full aggregate rebuild requested — delegated to scheduler");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void populateAggregate(ReviewRatingAggregate agg, List<Review> reviews) {
        int total    = reviews.size();
        int r1 = 0, r2 = 0, r3 = 0, r4 = 0, r5 = 0, verified = 0;

        for (Review r : reviews) {
            if (r.getRating() == null) continue;
            switch (r.getRating()) {
                case 1 -> r1++;
                case 2 -> r2++;
                case 3 -> r3++;
                case 4 -> r4++;
                case 5 -> r5++;
                default -> { /* ignore out-of-range */ }
            }
            if (Boolean.TRUE.equals(r.getVerifiedPurchase())) {
                verified++;
            }
        }

        agg.setReviewCount(total);
        agg.setRating1Count(r1);
        agg.setRating2Count(r2);
        agg.setRating3Count(r3);
        agg.setRating4Count(r4);
        agg.setRating5Count(r5);
        agg.setVerifiedCount(verified);
        agg.recalculateAverage();
    }
}

package ecommerce.modules.review.service.impl;

import ecommerce.modules.review.dto.ReviewSummaryResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewRatingAggregate;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewTargetType;
import ecommerce.modules.review.repository.ReviewRatingAggregateRepository;
import ecommerce.modules.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewAggregationServiceImpl")
class ReviewAggregationServiceImplTest {

    @Mock private ReviewRepository                reviewRepository;
    @Mock private ReviewRatingAggregateRepository reviewRatingAggregateRepository;

    @InjectMocks
    private ReviewAggregationServiceImpl service;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private Review publishedReview(int rating, boolean verified) {
        return Review.builder()
                .publicId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .productId(productId)
                .rating(rating)
                .status(ReviewStatus.PUBLISHED)
                .verifiedPurchase(verified)
                .build();
    }

    private ReviewRatingAggregate existingAggregate(UUID targetId) {
        return ReviewRatingAggregate.builder()
                .targetType(ReviewTargetType.PRODUCT)
                .targetId(targetId)
                .reviewCount(5)
                .rating5Count(5)
                .build();
    }

    // ── updateAggregateForProduct ─────────────────────────────────────────────

    @Nested
    @DisplayName("updateAggregateForProduct")
    class UpdateAggregateForProduct {

        @Test
        void noPublishedReviews_savesEmptyAggregate() {
            when(reviewRepository.findPublishedByProductId(productId)).thenReturn(List.of());
            when(reviewRatingAggregateRepository.findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, productId))
                    .thenReturn(Optional.empty());

            service.updateAggregateForProduct(productId);

            ArgumentCaptor<ReviewRatingAggregate> captor = ArgumentCaptor.forClass(ReviewRatingAggregate.class);
            verify(reviewRatingAggregateRepository).save(captor.capture());
            ReviewRatingAggregate saved = captor.getValue();
            assertThat(saved.getReviewCount()).isEqualTo(0);
            assertThat(saved.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void withPublishedReviews_calculatesCorrectly() {
            // 2×5-star + 1×3-star → count=3, sum=13, avg=4.33
            Review five1  = publishedReview(5, true);
            Review five2  = publishedReview(5, false);
            Review three1 = publishedReview(3, false);

            when(reviewRepository.findPublishedByProductId(productId)).thenReturn(List.of(five1, five2, three1));
            when(reviewRatingAggregateRepository.findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, productId))
                    .thenReturn(Optional.empty());

            service.updateAggregateForProduct(productId);

            ArgumentCaptor<ReviewRatingAggregate> captor = ArgumentCaptor.forClass(ReviewRatingAggregate.class);
            verify(reviewRatingAggregateRepository).save(captor.capture());
            ReviewRatingAggregate saved = captor.getValue();

            assertThat(saved.getReviewCount()).isEqualTo(3);
            assertThat(saved.getRating5Count()).isEqualTo(2);
            assertThat(saved.getRating3Count()).isEqualTo(1);
            assertThat(saved.getVerifiedCount()).isEqualTo(1);
            // (5+5+3)/3 = 4.33
            assertThat(saved.getAverageRating().doubleValue())
                    .isCloseTo(4.33, within(0.01));
        }

        @Test
        void existingAggregate_updatesInPlace() {
            ReviewRatingAggregate existing = existingAggregate(productId);
            Review five = publishedReview(5, true);

            when(reviewRepository.findPublishedByProductId(productId)).thenReturn(List.of(five));
            when(reviewRatingAggregateRepository.findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, productId))
                    .thenReturn(Optional.of(existing));

            service.updateAggregateForProduct(productId);

            ArgumentCaptor<ReviewRatingAggregate> captor = ArgumentCaptor.forClass(ReviewRatingAggregate.class);
            verify(reviewRatingAggregateRepository).save(captor.capture());
            // Should be the same object mutated, not a new one
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(existing.getReviewCount()).isEqualTo(1);
        }

        @Test
        void newAggregate_createsIt() {
            when(reviewRepository.findPublishedByProductId(productId)).thenReturn(List.of());
            when(reviewRatingAggregateRepository.findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, productId))
                    .thenReturn(Optional.empty());

            service.updateAggregateForProduct(productId);

            ArgumentCaptor<ReviewRatingAggregate> captor = ArgumentCaptor.forClass(ReviewRatingAggregate.class);
            verify(reviewRatingAggregateRepository).save(captor.capture());
            ReviewRatingAggregate saved = captor.getValue();
            assertThat(saved.getTargetType()).isEqualTo(ReviewTargetType.PRODUCT);
            assertThat(saved.getTargetId()).isEqualTo(productId);
        }
    }

    // ── getAggregate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAggregate")
    class GetAggregate {

        @Test
        void notFound_returnsEmptySummary() {
            UUID targetId = UUID.randomUUID();
            when(reviewRatingAggregateRepository.findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, targetId))
                    .thenReturn(Optional.empty());

            ReviewSummaryResponse result = service.getAggregate(ReviewTargetType.PRODUCT, targetId);

            assertThat(result.getReviewCount()).isEqualTo(0L);
            assertThat(result.getVerifiedCount()).isEqualTo(0L);
            assertThat(result.getAverageRating()).isNull();
        }

        @Test
        void found_mapsToDtoCorrectly() {
            UUID targetId = UUID.randomUUID();
            ReviewRatingAggregate agg = ReviewRatingAggregate.builder()
                    .targetType(ReviewTargetType.PRODUCT)
                    .targetId(targetId)
                    .reviewCount(10)
                    .averageRating(BigDecimal.valueOf(4.50))
                    .rating5Count(7)
                    .rating4Count(2)
                    .rating3Count(1)
                    .rating2Count(0)
                    .rating1Count(0)
                    .verifiedCount(6)
                    .build();

            when(reviewRatingAggregateRepository.findByTargetTypeAndTargetId(ReviewTargetType.PRODUCT, targetId))
                    .thenReturn(Optional.of(agg));

            ReviewSummaryResponse result = service.getAggregate(ReviewTargetType.PRODUCT, targetId);

            assertThat(result.getTargetId()).isEqualTo(targetId);
            assertThat(result.getTargetType()).isEqualTo(ReviewTargetType.PRODUCT);
            assertThat(result.getReviewCount()).isEqualTo(10L);
            assertThat(result.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.50));
            assertThat(result.getRating5Count()).isEqualTo(7L);
            assertThat(result.getRating4Count()).isEqualTo(2L);
            assertThat(result.getRating3Count()).isEqualTo(1L);
            assertThat(result.getVerifiedCount()).isEqualTo(6L);
            assertThat(result.getVerifiedPercentage()).isCloseTo(60.0, within(0.01));
        }
    }
}

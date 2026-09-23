package ecommerce.modules.review.policy;

import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewEditPolicy")
class ReviewEditPolicyTest {

    private ReviewEditPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ReviewEditPolicy();
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private Review publishedReview(UUID customerId, Instant publishedAt) {
        return Review.builder()
                .publicId(UUID.randomUUID())
                .customerId(customerId)
                .status(ReviewStatus.PUBLISHED)
                .publishedAt(publishedAt)
                .verifiedPurchase(false)
                .build();
    }

    private Review reviewWithStatus(UUID customerId, ReviewStatus status) {
        return Review.builder()
                .publicId(UUID.randomUUID())
                .customerId(customerId)
                .status(status)
                .verifiedPurchase(false)
                .build();
    }

    // ── canEdit ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canEdit")
    class CanEdit {

        @Test
        void ownReview_withinEditWindow_returnsTrue() {
            UUID userId = UUID.randomUUID();
            Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
            Review review = publishedReview(userId, tenDaysAgo);
            assertThat(policy.canEdit(review, userId)).isTrue();
        }

        @Test
        void notOwner_returnsFalse() {
            UUID owner = UUID.randomUUID();
            UUID stranger = UUID.randomUUID();
            Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
            Review review = publishedReview(owner, tenDaysAgo);
            assertThat(policy.canEdit(review, stranger)).isFalse();
        }

        @Test
        void deletedStatus_returnsFalse() {
            UUID userId = UUID.randomUUID();
            Review review = reviewWithStatus(userId, ReviewStatus.DELETED);
            assertThat(policy.canEdit(review, userId)).isFalse();
        }

        @Test
        void outsideEditWindow_returnsFalse() {
            UUID userId = UUID.randomUUID();
            Instant fortyDaysAgo = Instant.now().minus(40, ChronoUnit.DAYS);
            Review review = publishedReview(userId, fortyDaysAgo);
            assertThat(policy.canEdit(review, userId)).isFalse();
        }

        @Test
        void draftReview_noPublishedAt_returnsTrue() {
            UUID userId = UUID.randomUUID();
            Review review = Review.builder()
                    .publicId(UUID.randomUUID())
                    .customerId(userId)
                    .status(ReviewStatus.DRAFT)
                    .publishedAt(null)
                    .verifiedPurchase(false)
                    .build();
            assertThat(policy.canEdit(review, userId)).isTrue();
        }
    }
}

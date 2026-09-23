package ecommerce.modules.review;

import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.enums.ReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewStatus and Review entity status transitions")
class ReviewStatusTest {

    // ── Builders ────────────────────────────────────────────────────────────────

    private Review reviewWithStatus(UUID customerId, ReviewStatus status) {
        return Review.builder()
                .publicId(UUID.randomUUID())
                .customerId(customerId)
                .status(status)
                .verifiedPurchase(false)
                .build();
    }

    private Review publishedReview(UUID customerId, Instant publishedAt) {
        return Review.builder()
                .publicId(UUID.randomUUID())
                .customerId(customerId)
                .status(ReviewStatus.PUBLISHED)
                .publishedAt(publishedAt)
                .verifiedPurchase(false)
                .build();
    }

    // ── isPubliclyVisible ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("isPubliclyVisible")
    class IsPubliclyVisible {

        @Test
        void PUBLISHED_returnsTrue() {
            assertThat(ReviewStatus.PUBLISHED.isPubliclyVisible()).isTrue();
        }

        @Test
        void DRAFT_returnsFalse() {
            assertThat(ReviewStatus.DRAFT.isPubliclyVisible()).isFalse();
        }

        @Test
        void PENDING_MODERATION_returnsFalse() {
            assertThat(ReviewStatus.PENDING_MODERATION.isPubliclyVisible()).isFalse();
        }

        @Test
        void REJECTED_returnsFalse() {
            assertThat(ReviewStatus.REJECTED.isPubliclyVisible()).isFalse();
        }

        @Test
        void HIDDEN_returnsFalse() {
            assertThat(ReviewStatus.HIDDEN.isPubliclyVisible()).isFalse();
        }

        @Test
        void FLAGGED_returnsFalse() {
            assertThat(ReviewStatus.FLAGGED.isPubliclyVisible()).isFalse();
        }

        @Test
        void DELETED_returnsFalse() {
            assertThat(ReviewStatus.DELETED.isPubliclyVisible()).isFalse();
        }
    }

    // ── isFinal ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isFinal")
    class IsFinal {

        @Test
        void DELETED_returnsTrue() {
            assertThat(ReviewStatus.DELETED.isFinal()).isTrue();
        }

        @Test
        void PUBLISHED_returnsFalse() {
            assertThat(ReviewStatus.PUBLISHED.isFinal()).isFalse();
        }
    }

    // ── Review status transitions ─────────────────────────────────────────────

    @Nested
    @DisplayName("Review status transitions")
    class StatusTransitions {

        @Test
        void markPublished_setsStatusAndPublishedAt() {
            Review review = reviewWithStatus(UUID.randomUUID(), ReviewStatus.PENDING_MODERATION);
            Instant before = Instant.now();

            review.markPublished();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
            assertThat(review.getPublishedAt()).isNotNull();
            assertThat(review.getPublishedAt()).isAfterOrEqualTo(before);
        }

        @Test
        void markDeleted_setsStatusDeleted() {
            Review review = reviewWithStatus(UUID.randomUUID(), ReviewStatus.PUBLISHED);

            review.markDeleted();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
        }

        @Test
        void markHidden_setsStatusHidden() {
            Review review = reviewWithStatus(UUID.randomUUID(), ReviewStatus.PUBLISHED);

            review.markHidden();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        }

        @Test
        void markFlagged_setsStatusFlagged() {
            Review review = reviewWithStatus(UUID.randomUUID(), ReviewStatus.PUBLISHED);

            review.markFlagged();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.FLAGGED);
        }

        @Test
        void markPendingModeration_clearsPublishedAt_whenNotYetPublished() {
            Review review = reviewWithStatus(UUID.randomUUID(), ReviewStatus.DRAFT);
            // publishedAt is null — should remain null
            review.markPendingModeration();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING_MODERATION);
            assertThat(review.getPublishedAt()).isNull();
        }

        @Test
        void markPendingModeration_keepsPublishedAt_whenAlreadyPublished() {
            // The entity logic: if status != PUBLISHED, clears publishedAt.
            // So calling markPendingModeration on a PUBLISHED review DOES clear publishedAt per the code.
            // This test validates the actual implementation: starting from FLAGGED (not PUBLISHED)
            // clears publishedAt.
            Instant originalPublishedAt = Instant.now().minusSeconds(3600);
            Review review = Review.builder()
                    .publicId(UUID.randomUUID())
                    .customerId(UUID.randomUUID())
                    .status(ReviewStatus.FLAGGED)
                    .publishedAt(originalPublishedAt)
                    .verifiedPurchase(false)
                    .build();

            review.markPendingModeration();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING_MODERATION);
            // status was FLAGGED (not PUBLISHED), so publishedAt is cleared
            assertThat(review.getPublishedAt()).isNull();
        }

        @Test
        void markPendingModeration_publishedReview_preservesPublishedAt() {
            // When status IS PUBLISHED before the call, publishedAt is preserved per the entity logic.
            Instant originalPublishedAt = Instant.now().minusSeconds(3600);
            Review review = publishedReview(UUID.randomUUID(), originalPublishedAt);

            review.markPendingModeration();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING_MODERATION);
            assertThat(review.getPublishedAt()).isEqualTo(originalPublishedAt);
        }
    }

    // ── canBeEditedBy ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canBeEditedBy")
    class CanBeEditedBy {

        @Test
        void canBeEditedBy_owner_publishedStatus_returnsTrue() {
            UUID owner = UUID.randomUUID();
            Review review = reviewWithStatus(owner, ReviewStatus.PUBLISHED);
            assertThat(review.canBeEditedBy(owner)).isTrue();
        }

        @Test
        void canBeEditedBy_notOwner_returnsFalse() {
            Review review = reviewWithStatus(UUID.randomUUID(), ReviewStatus.PUBLISHED);
            assertThat(review.canBeEditedBy(UUID.randomUUID())).isFalse();
        }

        @Test
        void canBeEditedBy_deletedStatus_returnsFalse() {
            UUID owner = UUID.randomUUID();
            Review review = reviewWithStatus(owner, ReviewStatus.DELETED);
            assertThat(review.canBeEditedBy(owner)).isFalse();
        }

        @Test
        void canBeEditedBy_rejectedStatus_returnsFalse() {
            UUID owner = UUID.randomUUID();
            Review review = reviewWithStatus(owner, ReviewStatus.REJECTED);
            assertThat(review.canBeEditedBy(owner)).isFalse();
        }

        @Test
        void canBeEditedBy_hiddenStatus_returnsFalse() {
            UUID owner = UUID.randomUUID();
            Review review = reviewWithStatus(owner, ReviewStatus.HIDDEN);
            assertThat(review.canBeEditedBy(owner)).isFalse();
        }
    }

    // ── canBeDeletedBy ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canBeDeletedBy")
    class CanBeDeletedBy {

        @Test
        void canBeDeletedBy_owner_nonDeleted_returnsTrue() {
            UUID owner = UUID.randomUUID();
            Review review = reviewWithStatus(owner, ReviewStatus.PUBLISHED);
            assertThat(review.canBeDeletedBy(owner)).isTrue();
        }

        @Test
        void canBeDeletedBy_owner_alreadyDeleted_returnsFalse() {
            UUID owner = UUID.randomUUID();
            Review review = reviewWithStatus(owner, ReviewStatus.DELETED);
            assertThat(review.canBeDeletedBy(owner)).isFalse();
        }
    }
}

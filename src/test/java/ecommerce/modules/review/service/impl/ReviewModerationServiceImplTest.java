package ecommerce.modules.review.service.impl;

import ecommerce.modules.review.dto.ModerateReviewRequest;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewModeration;
import ecommerce.modules.review.enums.ReviewModerationAction;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.event.ReviewPublishedEvent;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewModerationServiceImpl")
class ReviewModerationServiceImplTest {

    @Mock private ReviewRepository                reviewRepository;
    @Mock private ReviewMediaRepository           reviewMediaRepository;
    @Mock private ReviewVoteRepository            reviewVoteRepository;
    @Mock private ReviewModerationRepository      reviewModerationRepository;
    @Mock private SellerReviewResponseRepository  sellerReviewResponseRepository;
    @Mock private ApplicationEventPublisher       eventPublisher;

    @InjectMocks
    private ReviewModerationServiceImpl service;

    private UUID reviewId;
    private UUID moderatorId;

    @BeforeEach
    void setUp() {
        reviewId    = UUID.randomUUID();
        moderatorId = UUID.randomUUID();
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private Review buildReview(ReviewStatus status) {
        Review review = Review.builder()
                .publicId(reviewId)
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .storeId(UUID.randomUUID())
                .rating(4)
                .status(status)
                .verifiedPurchase(false)
                .build();
        // Simulate the @PrePersist side-effect for id (needed by repo helpers)
        try {
            var field = Review.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(review, 1L);
        } catch (Exception ignored) { /* id stays null — acceptable for unit tests */ }
        return review;
    }

    private void stubToResponseDeps() {
        when(reviewVoteRepository.countByReview_IdAndVoteType(any(), eq(ReviewVoteType.HELPFUL))).thenReturn(0L);
        when(reviewVoteRepository.countByReview_IdAndVoteType(any(), eq(ReviewVoteType.NOT_HELPFUL))).thenReturn(0L);
        when(reviewMediaRepository.findByReview_IdOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(sellerReviewResponseRepository.findByReview_Id(any())).thenReturn(Optional.empty());
    }

    private ModerateReviewRequest requestFor(ReviewModerationAction action) {
        return ModerateReviewRequest.builder().action(action).reasonCode("test").notes("test notes").build();
    }

    // ── moderateReview ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moderateReview")
    class ModerateReview {

        @Test
        void reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.moderateReview(reviewId, requestFor(ReviewModerationAction.APPROVE), moderatorId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void action_APPROVE_callsMarkPublished_publishesEvent() {
            Review review = buildReview(ReviewStatus.PENDING_MODERATION);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.APPROVE), moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
            assertThat(review.getPublishedAt()).isNotNull();
            verify(eventPublisher).publishEvent(any(ReviewPublishedEvent.class));
        }

        @Test
        void action_REJECT_setsRejectedStatus() {
            Review review = buildReview(ReviewStatus.PENDING_MODERATION);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.REJECT), moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void action_HIDE_callsMarkHidden() {
            Review review = buildReview(ReviewStatus.PUBLISHED);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.HIDE), moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void action_FLAG_callsMarkFlagged() {
            Review review = buildReview(ReviewStatus.PUBLISHED);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.FLAG), moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.FLAGGED);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void action_RESTORE_callsMarkPublished_publishesEvent() {
            Review review = buildReview(ReviewStatus.HIDDEN);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.RESTORE), moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
            verify(eventPublisher).publishEvent(any(ReviewPublishedEvent.class));
        }

        @Test
        void action_ESCALATE_callsMarkFlagged() {
            Review review = buildReview(ReviewStatus.FLAGGED);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.ESCALATE), moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.FLAGGED);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void approve_recordsModerationEntry() {
            Review review = buildReview(ReviewStatus.PENDING_MODERATION);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.moderateReview(reviewId, requestFor(ReviewModerationAction.APPROVE), moderatorId);

            ArgumentCaptor<ReviewModeration> captor = ArgumentCaptor.forClass(ReviewModeration.class);
            verify(reviewModerationRepository).save(captor.capture());
            ReviewModeration logged = captor.getValue();
            assertThat(logged.getAction()).isEqualTo(ReviewModerationAction.APPROVE);
            assertThat(logged.getModeratorId()).isEqualTo(moderatorId);
        }
    }

    // ── hideReview ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hideReview")
    class HideReview {

        @Test
        void reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.hideReview(reviewId, "spam", moderatorId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void success_callsMarkHidden_savesModeration() {
            Review review = buildReview(ReviewStatus.PUBLISHED);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.hideReview(reviewId, "spam content", moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);

            ArgumentCaptor<ReviewModeration> captor = ArgumentCaptor.forClass(ReviewModeration.class);
            verify(reviewModerationRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(ReviewModerationAction.HIDE);
        }
    }

    // ── restoreReview ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("restoreReview")
    class RestoreReview {

        @Test
        void reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.restoreReview(reviewId, moderatorId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void success_callsMarkPublished_publishesEvent() {
            Review review = buildReview(ReviewStatus.HIDDEN);
            when(reviewRepository.findByPublicId(reviewId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(review)).thenReturn(review);
            stubToResponseDeps();

            service.restoreReview(reviewId, moderatorId);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
            assertThat(review.getPublishedAt()).isNotNull();

            ArgumentCaptor<ReviewModeration> captor = ArgumentCaptor.forClass(ReviewModeration.class);
            verify(reviewModerationRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(ReviewModerationAction.RESTORE);
        }
    }
}

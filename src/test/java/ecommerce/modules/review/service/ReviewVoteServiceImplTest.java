package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ReviewVoteRequest;
import ecommerce.modules.review.dto.ReviewVoteResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewVote;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.exception.ReviewAccessDeniedException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.repository.ReviewVoteRepository;
import ecommerce.modules.review.service.impl.ReviewVoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewVoteServiceImpl")
class ReviewVoteServiceImplTest {

    @Mock private ReviewRepository     reviewRepository;
    @Mock private ReviewVoteRepository reviewVoteRepository;

    @InjectMocks
    private ReviewVoteServiceImpl service;

    private UUID reviewPublicId;
    private UUID voterId;
    private UUID reviewOwnerId;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewPublicId  = UUID.randomUUID();
        voterId         = UUID.randomUUID();
        reviewOwnerId   = UUID.randomUUID();
        review          = buildReview(reviewOwnerId, 1L);
    }

    // ── Builder helpers ───────────────────────────────────────────────────────────

    private Review buildReview(UUID ownerId, Long id) {
        Review r = Review.builder()
                .customerId(ownerId)
                .productId(UUID.randomUUID())
                .rating(4)
                .status(ReviewStatus.PUBLISHED)
                .isActive(true)
                .build();
        // Reflectively set the DB id used in vote lookups
        try {
            java.lang.reflect.Field field = Review.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(r, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    private ReviewVote buildVote(Review review, UUID customerId, ReviewVoteType voteType) {
        return ReviewVote.builder()
                .review(review)
                .customerId(customerId)
                .voteType(voteType)
                .build();
    }

    // ── vote ─────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("vote")
    class Vote {

        @Test
        void vote_reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.vote(reviewPublicId,
                    new ReviewVoteRequest(ReviewVoteType.HELPFUL), voterId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining(reviewPublicId.toString());
        }

        @Test
        void vote_selfVote_throwsAccessDenied() {
            // voter is the same as the review owner
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> service.vote(reviewPublicId,
                    new ReviewVoteRequest(ReviewVoteType.HELPFUL), reviewOwnerId))
                    .isInstanceOf(ReviewAccessDeniedException.class)
                    .hasMessageContaining("cannot vote on your own review");
        }

        @Test
        void vote_newVote_createsVote() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(reviewVoteRepository.findByReview_IdAndCustomerId(review.getId(), voterId))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<ReviewVote> voteCaptor = ArgumentCaptor.forClass(ReviewVote.class);
            when(reviewVoteRepository.save(voteCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.HELPFUL))
                    .thenReturn(1L);
            when(reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.NOT_HELPFUL))
                    .thenReturn(0L);

            ReviewVoteResponse response = service.vote(reviewPublicId,
                    new ReviewVoteRequest(ReviewVoteType.HELPFUL), voterId);

            ReviewVote saved = voteCaptor.getValue();
            assertThat(saved.getVoteType()).isEqualTo(ReviewVoteType.HELPFUL);
            assertThat(saved.getCustomerId()).isEqualTo(voterId);
            assertThat(response.getHelpfulCount()).isEqualTo(1L);
            assertThat(response.getNotHelpfulCount()).isEqualTo(0L);
        }

        @Test
        void vote_changeVote_updatesExistingVote() {
            ReviewVote existing = buildVote(review, voterId, ReviewVoteType.HELPFUL);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(reviewVoteRepository.findByReview_IdAndCustomerId(review.getId(), voterId))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<ReviewVote> voteCaptor = ArgumentCaptor.forClass(ReviewVote.class);
            when(reviewVoteRepository.save(voteCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.HELPFUL))
                    .thenReturn(0L);
            when(reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.NOT_HELPFUL))
                    .thenReturn(1L);

            ReviewVoteResponse response = service.vote(reviewPublicId,
                    new ReviewVoteRequest(ReviewVoteType.NOT_HELPFUL), voterId);

            assertThat(voteCaptor.getValue().getVoteType()).isEqualTo(ReviewVoteType.NOT_HELPFUL);
            assertThat(response.getNotHelpfulCount()).isEqualTo(1L);
        }

        @Test
        void vote_sameVote_noChange() {
            ReviewVote existing = buildVote(review, voterId, ReviewVoteType.HELPFUL);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(reviewVoteRepository.findByReview_IdAndCustomerId(review.getId(), voterId))
                    .thenReturn(Optional.of(existing));

            when(reviewVoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.HELPFUL))
                    .thenReturn(1L);
            when(reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.NOT_HELPFUL))
                    .thenReturn(0L);

            // Same vote type as existing — service still calls save (upsert pattern), but vote type unchanged
            ReviewVoteResponse response = service.vote(reviewPublicId,
                    new ReviewVoteRequest(ReviewVoteType.HELPFUL), voterId);

            assertThat(existing.getVoteType()).isEqualTo(ReviewVoteType.HELPFUL);
            assertThat(response.getHelpfulCount()).isEqualTo(1L);
        }
    }

    // ── removeVote ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeVote")
    class RemoveVote {

        @Test
        void removeVote_reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeVote(reviewPublicId, voterId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining(reviewPublicId.toString());
        }

        @Test
        void removeVote_noVoteExists_doesNothing() {
            // The service always delegates to deleteByReview_IdAndCustomerId; the repo
            // is a no-op at DB level when no row matches — verify the service calls through.
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));

            service.removeVote(reviewPublicId, voterId);

            // deleteBy... is called regardless — repo handles the no-row case transparently
            verify(reviewVoteRepository).deleteByReview_IdAndCustomerId(review.getId(), voterId);
            verify(reviewVoteRepository, never()).save(any());
        }

        @Test
        void removeVote_voteExists_deletesIt() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));

            service.removeVote(reviewPublicId, voterId);

            verify(reviewVoteRepository).deleteByReview_IdAndCustomerId(review.getId(), voterId);
        }
    }
}

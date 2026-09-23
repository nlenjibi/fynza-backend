package ecommerce.modules.review.service.impl;

import ecommerce.modules.review.dto.SellerReviewResponseRequest;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.SellerReviewResponse;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.enums.SellerResponseStatus;
import ecommerce.modules.review.exception.ReviewAccessDeniedException;
import ecommerce.modules.review.exception.ReviewAlreadyExistsException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerReviewResponseServiceImpl")
class SellerReviewResponseServiceImplTest {

    @Mock private ReviewRepository               reviewRepository;
    @Mock private ReviewMediaRepository          reviewMediaRepository;
    @Mock private ReviewVoteRepository           reviewVoteRepository;
    @Mock private SellerReviewResponseRepository sellerReviewResponseRepository;

    @InjectMocks
    private SellerReviewResponseServiceImpl service;

    private UUID reviewPublicId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        reviewPublicId = UUID.randomUUID();
        sellerId       = UUID.randomUUID();
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private Review buildReview(UUID sellerIdOnReview) {
        Review review = Review.builder()
                .publicId(reviewPublicId)
                .customerId(UUID.randomUUID())
                .sellerId(sellerIdOnReview)
                .productId(UUID.randomUUID())
                .storeId(UUID.randomUUID())
                .rating(4)
                .status(ReviewStatus.PUBLISHED)
                .verifiedPurchase(false)
                .build();
        try {
            var field = Review.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(review, 1L);
        } catch (Exception ignored) { /* id stays null */ }
        return review;
    }

    private SellerReviewResponse buildSellerResponse(Review review, UUID respSellerId) {
        return SellerReviewResponse.builder()
                .review(review)
                .sellerId(respSellerId)
                .body("Original response body")
                .status(SellerResponseStatus.ACTIVE)
                .build();
    }

    private SellerReviewResponseRequest requestWithBody(String body) {
        return SellerReviewResponseRequest.builder().body(body).build();
    }

    /**
     * Stubs the helpers used by the private {@code toResponse()} method.
     * Does NOT stub {@code sellerReviewResponseRepository.findByReview_Id} — callers must set
     * that up themselves with the correct return value when it is also used in the primary flow
     * (updateResponse / deleteResponse).  For respondToReview the stub is included because the
     * primary flow uses {@code existsByReview_Id}, not {@code findByReview_Id}.
     */
    private void stubToResponseDepsNoSellerResponse() {
        when(reviewVoteRepository.countByReview_IdAndVoteType(any(), eq(ReviewVoteType.HELPFUL))).thenReturn(0L);
        when(reviewVoteRepository.countByReview_IdAndVoteType(any(), eq(ReviewVoteType.NOT_HELPFUL))).thenReturn(0L);
        when(reviewMediaRepository.findByReview_IdOrderBySortOrderAsc(any())).thenReturn(List.of());
    }

    private void stubToResponseDeps() {
        stubToResponseDepsNoSellerResponse();
        when(sellerReviewResponseRepository.findByReview_Id(any())).thenReturn(Optional.empty());
    }

    // ── respondToReview ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("respondToReview")
    class RespondToReview {

        @Test
        void reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.respondToReview(reviewPublicId, requestWithBody("body"), sellerId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void responseAlreadyExists_throwsAlreadyExists() {
            Review review = buildReview(sellerId);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(sellerReviewResponseRepository.existsByReview_Id(review.getId())).thenReturn(true);

            assertThatThrownBy(() -> service.respondToReview(reviewPublicId, requestWithBody("body"), sellerId))
                    .isInstanceOf(ReviewAlreadyExistsException.class);
        }

        @Test
        void success_createsResponse() {
            Review review = buildReview(sellerId);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(sellerReviewResponseRepository.existsByReview_Id(review.getId())).thenReturn(false);
            stubToResponseDeps();

            service.respondToReview(reviewPublicId, requestWithBody("Great product!"), sellerId);

            ArgumentCaptor<SellerReviewResponse> captor = ArgumentCaptor.forClass(SellerReviewResponse.class);
            verify(sellerReviewResponseRepository).save(captor.capture());
            SellerReviewResponse saved = captor.getValue();
            assertThat(saved.getSellerId()).isEqualTo(sellerId);
            assertThat(saved.getBody()).isEqualTo("Great product!");
            assertThat(saved.getStatus()).isEqualTo(SellerResponseStatus.ACTIVE);
        }
    }

    // ── updateResponse ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateResponse")
    class UpdateResponse {

        @Test
        void reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateResponse(reviewPublicId, requestWithBody("new body"), sellerId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void noExistingResponse_throwsNotFound() {
            Review review = buildReview(sellerId);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(sellerReviewResponseRepository.findByReview_Id(review.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateResponse(reviewPublicId, requestWithBody("new body"), sellerId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void wrongSeller_throwsAccessDenied() {
            Review review = buildReview(sellerId);
            UUID differentSeller = UUID.randomUUID();
            SellerReviewResponse existing = buildSellerResponse(review, differentSeller);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(sellerReviewResponseRepository.findByReview_Id(review.getId())).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateResponse(reviewPublicId, requestWithBody("new body"), sellerId))
                    .isInstanceOf(ReviewAccessDeniedException.class);
        }

        @Test
        void success_updatesBody() {
            Review review = buildReview(sellerId);
            SellerReviewResponse existing = buildSellerResponse(review, sellerId);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            // First call: primary flow finds the existing response.
            // Second call: toResponse() renders the DTO — return empty so no nested DTO is built.
            when(sellerReviewResponseRepository.findByReview_Id(review.getId()))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());
            stubToResponseDepsNoSellerResponse();

            service.updateResponse(reviewPublicId, requestWithBody("Updated body"), sellerId);

            assertThat(existing.getBody()).isEqualTo("Updated body");
            verify(sellerReviewResponseRepository).save(existing);
        }
    }

    // ── deleteResponse ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteResponse")
    class DeleteResponse {

        @Test
        void reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteResponse(reviewPublicId, sellerId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void noExistingResponse_throwsNotFound() {
            // The real implementation throws ReviewNotFoundException when the response is missing.
            Review review = buildReview(sellerId);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(sellerReviewResponseRepository.findByReview_Id(review.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteResponse(reviewPublicId, sellerId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        void success_setsStatusDeleted() {
            Review review = buildReview(sellerId);
            SellerReviewResponse existing = buildSellerResponse(review, sellerId);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            // First call: primary flow finds the existing response.
            // Second call: toResponse() renders the DTO — return empty so no nested DTO is built.
            when(sellerReviewResponseRepository.findByReview_Id(review.getId()))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());
            stubToResponseDepsNoSellerResponse();

            service.deleteResponse(reviewPublicId, sellerId);

            assertThat(existing.getStatus()).isEqualTo(SellerResponseStatus.DELETED);
            verify(sellerReviewResponseRepository).save(existing);
        }
    }
}

package ecommerce.modules.review.service;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.OrderItem;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.review.dto.CreateReviewRequest;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.UpdateReviewRequest;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewAuditLog;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.event.ReviewCreatedEvent;
import ecommerce.modules.review.exception.ReviewAccessDeniedException;
import ecommerce.modules.review.exception.ReviewAlreadyExistsException;
import ecommerce.modules.review.exception.ReviewNotEligibleException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.policy.ReviewEditPolicy;
import ecommerce.modules.review.policy.ReviewEligibilityPolicy;
import ecommerce.modules.review.policy.ReviewModerationPolicy;
import ecommerce.modules.review.repository.ReviewAuditLogRepository;
import ecommerce.modules.review.repository.ReviewMediaRepository;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.repository.ReviewVoteRepository;
import ecommerce.modules.review.repository.SellerReviewResponseRepository;
import ecommerce.modules.review.service.ReviewAggregationService;
import ecommerce.modules.review.service.impl.ReviewServiceImpl;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
@DisplayName("ReviewServiceImpl")
class ReviewServiceImplTest {

    @Mock private ReviewRepository              reviewRepository;
    @Mock private ReviewMediaRepository         reviewMediaRepository;
    @Mock private ReviewVoteRepository          reviewVoteRepository;
    @Mock private ReviewAuditLogRepository      reviewAuditLogRepository;
    @Mock private SellerReviewResponseRepository sellerReviewResponseRepository;
    @Mock private OrderRepository               orderRepository;
    @Mock private ReviewEligibilityPolicy       eligibilityPolicy;
    @Mock private ReviewEditPolicy              editPolicy;
    @Mock private ReviewModerationPolicy        moderationPolicy;
    @Mock private ReviewAggregationService      aggregationService;
    @Mock private ApplicationEventPublisher     eventPublisher;

    @InjectMocks
    private ReviewServiceImpl service;

    private UUID customerId;
    private UUID productId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID reviewPublicId;

    @BeforeEach
    void setUp() {
        customerId    = UUID.randomUUID();
        productId     = UUID.randomUUID();
        orderId       = UUID.randomUUID();
        orderItemId   = UUID.randomUUID();
        reviewPublicId = UUID.randomUUID();
    }

    // ── Builder helpers ──────────────────────────────────────────────────────────

    private Review buildReview() {
        return Review.builder()
                .customerId(customerId)
                .productId(productId)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(4)
                .title("Great product")
                .body("Really liked it")
                .status(ReviewStatus.PENDING_MODERATION)
                .verifiedPurchase(true)
                .isActive(true)
                .build();
    }

    private Review buildPublishedReview() {
        return Review.builder()
                .customerId(customerId)
                .productId(productId)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(4)
                .title("Great product")
                .body("Really liked it")
                .status(ReviewStatus.PUBLISHED)
                .verifiedPurchase(true)
                .isActive(true)
                .publishedAt(Instant.now().minusSeconds(3600))
                .build();
    }

    private CreateReviewRequest buildCreateRequest() {
        return CreateReviewRequest.builder()
                .productId(productId)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(4)
                .title("Great product")
                .body("Really liked it")
                .build();
    }

    private Order buildMockOrder(UUID ownerId, OrderStatus status) {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getPublicId()).thenReturn(orderId);
        when(order.getCustomerId()).thenReturn(ownerId);
        when(order.getStatus()).thenReturn(status);
        when(order.getCreatedAt()).thenReturn(Instant.now().minusSeconds(86400));

        OrderItem item = org.mockito.Mockito.mock(OrderItem.class);
        when(item.getPublicId()).thenReturn(orderItemId);
        when(item.getProductId()).thenReturn(productId);
        when(order.getOrderItems()).thenReturn(List.of(item));

        return order;
    }

    private void stubToResponseDependencies(Review review) {
        when(reviewVoteRepository.countByReview_IdAndVoteType(any(), eq(ReviewVoteType.HELPFUL))).thenReturn(0L);
        when(reviewVoteRepository.countByReview_IdAndVoteType(any(), eq(ReviewVoteType.NOT_HELPFUL))).thenReturn(0L);
        when(reviewMediaRepository.findByReview_IdOrderBySortOrderAsc(any())).thenReturn(Collections.emptyList());
        when(sellerReviewResponseRepository.findByReview_Id(any())).thenReturn(Optional.empty());
    }

    // ── createReview ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createReview")
    class CreateReview {

        @Test
        void createReview_orderNotFound_throwsNotEligible() {
            when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createReview(buildCreateRequest(), customerId))
                    .isInstanceOf(ReviewNotEligibleException.class)
                    .hasMessageContaining("Order not found");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        void createReview_notOrderOwner_throwsNotEligible() {
            Order order = buildMockOrder(UUID.randomUUID(), OrderStatus.DELIVERED);
            when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
            when(eligibilityPolicy.isOwner(order, customerId)).thenReturn(false);

            assertThatThrownBy(() -> service.createReview(buildCreateRequest(), customerId))
                    .isInstanceOf(ReviewAccessDeniedException.class)
                    .hasMessageContaining("Order does not belong to this customer");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        void createReview_orderNotEligible_throwsNotEligible() {
            Order order = buildMockOrder(customerId, OrderStatus.PENDING);
            when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
            when(eligibilityPolicy.isOwner(order, customerId)).thenReturn(true);
            when(eligibilityPolicy.isOrderEligibleForReview(order)).thenReturn(false);

            assertThatThrownBy(() -> service.createReview(buildCreateRequest(), customerId))
                    .isInstanceOf(ReviewNotEligibleException.class)
                    .hasMessageContaining("Order status is not eligible for review");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        void createReview_duplicateReview_throwsAlreadyExists() {
            Order order = buildMockOrder(customerId, OrderStatus.DELIVERED);
            when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
            when(eligibilityPolicy.isOwner(order, customerId)).thenReturn(true);
            when(eligibilityPolicy.isOrderEligibleForReview(order)).thenReturn(true);
            when(eligibilityPolicy.isWithinReviewWindow(order)).thenReturn(true);
            when(eligibilityPolicy.orderContainsItem(order, orderItemId)).thenReturn(true);
            when(reviewRepository.existsByProductIdAndCustomerIdAndOrderItemId(productId, customerId, orderItemId))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createReview(buildCreateRequest(), customerId))
                    .isInstanceOf(ReviewAlreadyExistsException.class)
                    .hasMessageContaining("review already exists");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        void createReview_success_savesReviewAndPublishesEvent() {
            Order order = buildMockOrder(customerId, OrderStatus.DELIVERED);
            when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
            when(eligibilityPolicy.isOwner(order, customerId)).thenReturn(true);
            when(eligibilityPolicy.isOrderEligibleForReview(order)).thenReturn(true);
            when(eligibilityPolicy.isWithinReviewWindow(order)).thenReturn(true);
            when(eligibilityPolicy.orderContainsItem(order, orderItemId)).thenReturn(true);
            when(reviewRepository.existsByProductIdAndCustomerIdAndOrderItemId(productId, customerId, orderItemId))
                    .thenReturn(false);
            when(moderationPolicy.requiresModeration(any())).thenReturn(true);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            when(reviewRepository.save(reviewCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewAuditLogRepository.save(any(ReviewAuditLog.class))).thenAnswer(i -> i.getArgument(0));
            stubToResponseDependencies(null);

            ReviewResponse response = service.createReview(buildCreateRequest(), customerId);

            Review saved = reviewCaptor.getValue();
            assertThat(saved.getCustomerId()).isEqualTo(customerId);
            assertThat(saved.getProductId()).isEqualTo(productId);
            assertThat(saved.getRating()).isEqualTo(4);
            assertThat(saved.getStatus()).isEqualTo(ReviewStatus.PENDING_MODERATION);

            verify(eventPublisher).publishEvent(any(ReviewCreatedEvent.class));
            assertThat(response).isNotNull();
        }

        @Test
        void createReview_success_setsVerifiedPurchaseTrue() {
            Order order = buildMockOrder(customerId, OrderStatus.DELIVERED);
            when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
            when(eligibilityPolicy.isOwner(order, customerId)).thenReturn(true);
            when(eligibilityPolicy.isOrderEligibleForReview(order)).thenReturn(true);
            when(eligibilityPolicy.isWithinReviewWindow(order)).thenReturn(true);
            when(eligibilityPolicy.orderContainsItem(order, orderItemId)).thenReturn(true);
            when(reviewRepository.existsByProductIdAndCustomerIdAndOrderItemId(productId, customerId, orderItemId))
                    .thenReturn(false);
            when(moderationPolicy.requiresModeration(any())).thenReturn(true);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            when(reviewRepository.save(reviewCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewAuditLogRepository.save(any(ReviewAuditLog.class))).thenAnswer(i -> i.getArgument(0));
            stubToResponseDependencies(null);

            service.createReview(buildCreateRequest(), customerId);

            assertThat(reviewCaptor.getValue().getVerifiedPurchase()).isTrue();
        }
    }

    // ── updateReview ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateReview")
    class UpdateReview {

        @Test
        void updateReview_reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateReview(reviewPublicId,
                    UpdateReviewRequest.builder().rating(5).build(), customerId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining(reviewPublicId.toString());
        }

        @Test
        void updateReview_notOwner_throwsAccessDenied() {
            Review review = buildReview();
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(editPolicy.canEdit(review, customerId)).thenReturn(false);

            assertThatThrownBy(() -> service.updateReview(reviewPublicId,
                    UpdateReviewRequest.builder().rating(5).build(), customerId))
                    .isInstanceOf(ReviewAccessDeniedException.class)
                    .hasMessageContaining("cannot be edited");
        }

        @Test
        void updateReview_editPolicyDenied_throwsAccessDenied() {
            Review review = buildPublishedReview();
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(editPolicy.canEdit(review, customerId)).thenReturn(false);

            assertThatThrownBy(() -> service.updateReview(reviewPublicId,
                    UpdateReviewRequest.builder().body("updated body").build(), customerId))
                    .isInstanceOf(ReviewAccessDeniedException.class);
        }

        @Test
        void updateReview_publishedReview_setsEditedAtAndPendingModeration() {
            Review review = buildPublishedReview();
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(editPolicy.canEdit(review, customerId)).thenReturn(true);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            when(reviewRepository.save(reviewCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewAuditLogRepository.save(any(ReviewAuditLog.class))).thenAnswer(i -> i.getArgument(0));
            stubToResponseDependencies(review);

            service.updateReview(reviewPublicId,
                    UpdateReviewRequest.builder().body("updated body").build(), customerId);

            Review saved = reviewCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ReviewStatus.PENDING_MODERATION);
            assertThat(saved.getEditedAt()).isNotNull();
        }

        @Test
        void updateReview_updatesNonNullFields() {
            Review review = buildReview();
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(editPolicy.canEdit(review, customerId)).thenReturn(true);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            when(reviewRepository.save(reviewCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewAuditLogRepository.save(any(ReviewAuditLog.class))).thenAnswer(i -> i.getArgument(0));
            stubToResponseDependencies(review);

            String originalTitle = review.getTitle();
            UpdateReviewRequest request = UpdateReviewRequest.builder()
                    .rating(5)
                    .body("new body")
                    // title is intentionally null → must not overwrite
                    .build();

            service.updateReview(reviewPublicId, request, customerId);

            Review saved = reviewCaptor.getValue();
            assertThat(saved.getRating()).isEqualTo(5);
            assertThat(saved.getBody()).isEqualTo("new body");
            assertThat(saved.getTitle()).isEqualTo(originalTitle);
        }
    }

    // ── deleteReview ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteReview")
    class DeleteReview {

        @Test
        void deleteReview_reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteReview(reviewPublicId, customerId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining(reviewPublicId.toString());
        }

        @Test
        void deleteReview_notOwner_throwsAccessDenied() {
            UUID otherId = UUID.randomUUID();
            Review review = Review.builder()
                    .customerId(otherId)
                    .productId(productId)
                    .status(ReviewStatus.PUBLISHED)
                    .isActive(true)
                    .build();

            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> service.deleteReview(reviewPublicId, customerId))
                    .isInstanceOf(ReviewAccessDeniedException.class)
                    .hasMessageContaining("cannot be deleted");
        }

        @Test
        void deleteReview_success_callsMarkDeleted() {
            Review review = buildReview();
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            when(reviewRepository.save(reviewCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(reviewAuditLogRepository.save(any(ReviewAuditLog.class))).thenAnswer(i -> i.getArgument(0));

            service.deleteReview(reviewPublicId, customerId);

            Review saved = reviewCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ReviewStatus.DELETED);
            verify(reviewRepository).save(review);
        }
    }

    // ── getReview ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getReview")
    class GetReview {

        @Test
        void getReview_notFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getReview(reviewPublicId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining(reviewPublicId.toString());
        }

        @Test
        void getReview_found_returnsResponse() {
            Review review = buildReview();
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            stubToResponseDependencies(review);

            ReviewResponse response = service.getReview(reviewPublicId);

            assertThat(response).isNotNull();
            assertThat(response.getCustomerId()).isEqualTo(customerId);
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getRating()).isEqualTo(4);
        }
    }
}

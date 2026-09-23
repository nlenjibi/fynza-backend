package ecommerce.modules.review.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.OrderItem;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.review.dto.*;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewAuditLog;
import ecommerce.modules.review.entity.ReviewMedia;
import ecommerce.modules.review.entity.SellerReviewResponse;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewTargetType;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.event.ReviewCreatedEvent;
import ecommerce.modules.review.exception.ReviewAccessDeniedException;
import ecommerce.modules.review.exception.ReviewAlreadyExistsException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.exception.ReviewNotEligibleException;
import ecommerce.modules.review.policy.ReviewEditPolicy;
import ecommerce.modules.review.policy.ReviewEligibilityPolicy;
import ecommerce.modules.review.policy.ReviewModerationPolicy;
import ecommerce.modules.review.repository.*;
import ecommerce.modules.review.service.ReviewAggregationService;
import ecommerce.modules.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ReviewAuditLogRepository reviewAuditLogRepository;
    private final SellerReviewResponseRepository sellerReviewResponseRepository;
    private final OrderRepository orderRepository;
    private final ReviewEligibilityPolicy eligibilityPolicy;
    private final ReviewEditPolicy editPolicy;
    private final ReviewModerationPolicy moderationPolicy;
    private final ReviewAggregationService aggregationService;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private ReviewResponse toResponse(Review review) {
        long helpfulCount = reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.HELPFUL);
        long notHelpfulCount = reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.NOT_HELPFUL);

        List<ReviewMedia> mediaList = reviewMediaRepository.findByReview_IdOrderBySortOrderAsc(review.getId());
        List<ReviewMediaResponse> mediaResponses = mediaList.stream()
                .map(m -> ReviewMediaResponse.builder()
                        .id(m.getPublicId())
                        .mediaReference(m.getMediaReference())
                        .mediaType(m.getMediaType())
                        .sortOrder(m.getSortOrder())
                        .build())
                .toList();

        SellerResponseDTO sellerResponseDTO = null;
        Optional<SellerReviewResponse> sellerResponse = sellerReviewResponseRepository.findByReview_Id(review.getId());
        if (sellerResponse.isPresent()) {
            SellerReviewResponse sr = sellerResponse.get();
            sellerResponseDTO = SellerResponseDTO.builder()
                    .id(sr.getPublicId())
                    .sellerId(sr.getSellerId())
                    .body(sr.getBody())
                    .status(sr.getStatus())
                    .createdAt(sr.getCreatedAt())
                    .updatedAt(sr.getUpdatedAt())
                    .build();
        }

        return ReviewResponse.builder()
                .id(review.getPublicId())
                .customerId(review.getCustomerId())
                .productId(review.getProductId())
                .variantId(review.getVariantId())
                .storeId(review.getStoreId())
                .sellerId(review.getSellerId())
                .orderId(review.getOrderId())
                .orderItemId(review.getOrderItemId())
                .status(review.getStatus())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .verifiedPurchase(review.getVerifiedPurchase())
                .editedAt(review.getEditedAt())
                .publishedAt(review.getPublishedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .helpfulCount(helpfulCount)
                .notHelpfulCount(notHelpfulCount)
                .media(mediaResponses)
                .sellerResponse(sellerResponseDTO)
                .build();
    }

    private ReviewPageResponse toPageResponse(Page<Review> page) {
        return ReviewPageResponse.builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    private void logAudit(Review review, UUID actorId, String action, String oldValue, String newValue) {
        ReviewAuditLog log = ReviewAuditLog.builder()
                .review(review)
                .actorId(actorId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        reviewAuditLogRepository.save(log);
    }

    // ─── createReview ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEWS,      allEntries = true),
            @CacheEvict(value = CacheNames.REVIEW_STATS, allEntries = true),
            @CacheEvict(value = CacheNames.USER_REVIEWS, allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_REVIEWS, allEntries = true)
    })
    public ReviewResponse createReview(CreateReviewRequest request, UUID customerId) {
        log.info("Creating review for product={} orderItem={} customer={}", request.getProductId(), request.getOrderItemId(), customerId);

        Order order = orderRepository.findByPublicId(request.getOrderId())
                .orElseThrow(() -> new ReviewNotEligibleException("Order not found: " + request.getOrderId()));

        if (!eligibilityPolicy.isOwner(order, customerId)) {
            throw new ReviewAccessDeniedException("Order does not belong to this customer");
        }
        if (!eligibilityPolicy.isOrderEligibleForReview(order)) {
            throw new ReviewNotEligibleException("Order status is not eligible for review: " + order.getStatus());
        }
        if (!eligibilityPolicy.isWithinReviewWindow(order)) {
            throw new ReviewNotEligibleException("Review window of 90 days has passed for this order");
        }
        if (!eligibilityPolicy.orderContainsItem(order, request.getOrderItemId())) {
            throw new ReviewNotEligibleException("Order item not found in this order");
        }

        if (reviewRepository.existsByProductIdAndCustomerIdAndOrderItemId(
                request.getProductId(), customerId, request.getOrderItemId())) {
            throw new ReviewAlreadyExistsException(
                    "A review already exists for this product and order item");
        }

        OrderItem orderItem = order.getOrderItems().stream()
                .filter(i -> request.getOrderItemId().equals(i.getPublicId()))
                .findFirst()
                .orElseThrow(() -> new ReviewNotEligibleException("Order item not found"));

        Review review = Review.builder()
                .customerId(customerId)
                .productId(request.getProductId())
                .variantId(request.getVariantId())
                .storeId(request.getStoreId())
                .orderId(order.getPublicId())
                .orderItemId(request.getOrderItemId())
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .verifiedPurchase(true)
                .status(ReviewStatus.PENDING_MODERATION)
                .build();

        if (!moderationPolicy.requiresModeration(review)) {
            review.markPublished();
        }

        Review saved = reviewRepository.save(review);
        logAudit(saved, customerId, "CREATE", null, "rating=" + saved.getRating() + ",status=" + saved.getStatus());

        eventPublisher.publishEvent(new ReviewCreatedEvent(
                saved.getPublicId(),
                customerId,
                saved.getProductId(),
                saved.getStoreId(),
                saved.getSellerId(),
                saved.getRating()
        ));

        log.info("Review created: publicId={}", saved.getPublicId());
        return toResponse(saved);
    }

    // ─── updateReview ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,       key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS,      allEntries = true),
            @CacheEvict(value = CacheNames.REVIEW_STATS, allEntries = true),
            @CacheEvict(value = CacheNames.USER_REVIEWS, allEntries = true)
    })
    public ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, UUID customerId) {
        log.info("Updating review={} customer={}", reviewId, customerId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        if (!editPolicy.canEdit(review, customerId)) {
            throw new ReviewAccessDeniedException("Review cannot be edited by this user or edit window has expired");
        }

        String oldState = "rating=" + review.getRating() + ",status=" + review.getStatus();

        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getTitle()  != null) review.setTitle(request.getTitle());
        if (request.getBody()   != null) review.setBody(request.getBody());

        if (review.getStatus() == ReviewStatus.PUBLISHED) {
            review.setEditedAt(java.time.Instant.now());
            review.markPendingModeration();
        }

        Review updated = reviewRepository.save(review);
        logAudit(updated, customerId, "UPDATE", oldState, "rating=" + updated.getRating() + ",status=" + updated.getStatus());

        log.info("Review updated: publicId={}", updated.getPublicId());
        return toResponse(updated);
    }

    // ─── deleteReview ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,       key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS,      allEntries = true),
            @CacheEvict(value = CacheNames.REVIEW_STATS, allEntries = true),
            @CacheEvict(value = CacheNames.USER_REVIEWS, allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_REVIEWS, allEntries = true)
    })
    public void deleteReview(UUID reviewId, UUID customerId) {
        log.info("Deleting review={} customer={}", reviewId, customerId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        if (!review.canBeDeletedBy(customerId)) {
            throw new ReviewAccessDeniedException("Review cannot be deleted by this user");
        }

        String oldStatus = review.getStatus().name();
        review.markDeleted();
        reviewRepository.save(review);
        logAudit(review, customerId, "DELETE", oldStatus, ReviewStatus.DELETED.name());

        log.info("Review deleted: publicId={}", reviewId);
    }

    // ─── getReview ────────────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.REVIEW, key = "#reviewId")
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));
        return toResponse(review);
    }

    // ─── getProductReviews ────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.REVIEWS,
               key = "'product:' + #productId + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public ReviewPageResponse getProductReviews(UUID productId, ReviewStatus status, Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductIdAndStatus(productId, status, pageable);
        return toPageResponse(page);
    }

    // ─── getStoreReviews ──────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.REVIEWS,
               key = "'store:' + #storeId + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public ReviewPageResponse getStoreReviews(UUID storeId, ReviewStatus status, Pageable pageable) {
        Page<Review> page = reviewRepository.findByStoreIdAndStatus(storeId, status, pageable);
        return toPageResponse(page);
    }

    // ─── getMyReviews ─────────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.USER_REVIEWS,
               key = "'customer:' + #customerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public ReviewPageResponse getMyReviews(UUID customerId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByCustomerId(customerId, pageable);
        return toPageResponse(page);
    }

    // ─── getAdminReviews ──────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.ADMIN_REVIEWS,
               key = "'status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public ReviewPageResponse getAdminReviews(ReviewStatus status, Pageable pageable) {
        Page<Review> page = reviewRepository.findByStatus(status, pageable);
        return toPageResponse(page);
    }

    // ─── getReviewSummary ─────────────────────────────────────────────────────

    @Override
    @Cacheable(value = CacheNames.REVIEW_STATS,
               key = "#targetType + ':' + #targetId")
    public ReviewSummaryResponse getReviewSummary(ReviewTargetType targetType, UUID targetId) {
        return aggregationService.getAggregate(targetType, targetId);
    }

    // ─── checkEligibility ─────────────────────────────────────────────────────

    @Override
    public ReviewEligibilityResponse checkEligibility(UUID orderId, UUID orderItemId, UUID customerId) {
        Optional<Order> orderOpt = orderRepository.findByPublicId(orderId);
        if (orderOpt.isEmpty()) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .reason("Order not found")
                    .build();
        }

        Order order = orderOpt.get();

        if (!eligibilityPolicy.isOwner(order, customerId)) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .reason("Order does not belong to this customer")
                    .build();
        }
        if (!eligibilityPolicy.isOrderEligibleForReview(order)) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .reason("Order status is not eligible for review: " + order.getStatus())
                    .build();
        }
        if (!eligibilityPolicy.isWithinReviewWindow(order)) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .reason("The 90-day review window has expired")
                    .build();
        }
        if (!eligibilityPolicy.orderContainsItem(order, orderItemId)) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .reason("Order item not found in this order")
                    .build();
        }

        OrderItem item = order.getOrderItems().stream()
                .filter(i -> orderItemId.equals(i.getPublicId()))
                .findFirst()
                .orElse(null);

        UUID productId = item != null ? item.getProductId() : null;
        boolean alreadyReviewed = productId != null &&
                reviewRepository.existsByProductIdAndCustomerIdAndOrderItemId(productId, customerId, orderItemId);

        if (alreadyReviewed) {
            Review existing = reviewRepository.findByPublicId(orderId).orElse(null);
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .reason("You have already submitted a review for this item")
                    .existingReviewId(existing != null ? existing.getPublicId() : null)
                    .productEligible(false)
                    .storeEligible(true)
                    .build();
        }

        return ReviewEligibilityResponse.builder()
                .eligible(true)
                .reason("Eligible to review")
                .productEligible(true)
                .storeEligible(true)
                .build();
    }
}

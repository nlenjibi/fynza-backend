package ecommerce.modules.review.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.review.dto.ModerateReviewRequest;
import ecommerce.modules.review.dto.ReviewMediaResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.SellerResponseDTO;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewMedia;
import ecommerce.modules.review.entity.ReviewModeration;
import ecommerce.modules.review.entity.SellerReviewResponse;
import ecommerce.modules.review.enums.ReviewModerationAction;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.*;
import ecommerce.modules.review.event.ReviewPublishedEvent;
import ecommerce.modules.review.service.ReviewModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewModerationServiceImpl implements ReviewModerationService {

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ReviewModerationRepository reviewModerationRepository;
    private final SellerReviewResponseRepository sellerReviewResponseRepository;
    private final ApplicationEventPublisher eventPublisher;

    private ReviewResponse toResponse(Review review) {
        long helpfulCount    = reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.HELPFUL);
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
        Optional<SellerReviewResponse> srOpt = sellerReviewResponseRepository.findByReview_Id(review.getId());
        if (srOpt.isPresent()) {
            SellerReviewResponse sr = srOpt.get();
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

    private void recordModeration(Review review, ReviewModerationAction action, String reasonCode,
                                  String notes, UUID moderatorId) {
        ReviewModeration entry = ReviewModeration.builder()
                .review(review)
                .action(action)
                .reasonCode(reasonCode)
                .notes(notes)
                .moderatorId(moderatorId)
                .build();
        reviewModerationRepository.save(entry);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,        key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS,       allEntries = true),
            @CacheEvict(value = CacheNames.REVIEW_STATS,  allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_REVIEWS, allEntries = true)
    })
    public ReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request, UUID moderatorId) {
        log.info("Moderate review={} action={} moderator={}", reviewId, request.getAction(), moderatorId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        ReviewModerationAction action = request.getAction();
        switch (action) {
            case APPROVE  -> review.markPublished();
            case REJECT   -> review.setStatus(ReviewStatus.REJECTED);
            case HIDE     -> review.markHidden();
            case FLAG     -> review.markFlagged();
            case RESTORE  -> review.markPublished();
            case ESCALATE -> review.markFlagged();
        }

        Review saved = reviewRepository.save(review);
        recordModeration(saved, action, request.getReasonCode(), request.getNotes(), moderatorId);

        if (action == ReviewModerationAction.APPROVE || action == ReviewModerationAction.RESTORE) {
            eventPublisher.publishEvent(new ReviewPublishedEvent(
                    saved.getPublicId(), saved.getCustomerId(), saved.getProductId(),
                    saved.getStoreId(), saved.getRating(), Boolean.TRUE.equals(saved.getVerifiedPurchase())));
        }

        log.info("Review={} moderated to status={}", reviewId, saved.getStatus());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,        key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS,       allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_REVIEWS, allEntries = true)
    })
    public ReviewResponse hideReview(UUID reviewId, String reason, UUID moderatorId) {
        log.info("Hide review={} moderator={}", reviewId, moderatorId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        review.markHidden();
        Review saved = reviewRepository.save(review);
        recordModeration(saved, ReviewModerationAction.HIDE, null, reason, moderatorId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,        key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS,       allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_REVIEWS, allEntries = true)
    })
    public ReviewResponse restoreReview(UUID reviewId, UUID moderatorId) {
        log.info("Restore review={} moderator={}", reviewId, moderatorId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        review.markPublished();
        Review saved = reviewRepository.save(review);
        recordModeration(saved, ReviewModerationAction.RESTORE, null, "Restored by moderator", moderatorId);

        return toResponse(saved);
    }
}

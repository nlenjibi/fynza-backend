package ecommerce.modules.review.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.review.dto.ReviewMediaResponse;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.SellerResponseDTO;
import ecommerce.modules.review.dto.SellerReviewResponseRequest;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewMedia;
import ecommerce.modules.review.entity.SellerReviewResponse;
import ecommerce.modules.review.enums.SellerResponseStatus;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.exception.ReviewAccessDeniedException;
import ecommerce.modules.review.exception.ReviewAlreadyExistsException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.*;
import ecommerce.modules.review.service.SellerReviewResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerReviewResponseServiceImpl implements SellerReviewResponseService {

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final SellerReviewResponseRepository sellerReviewResponseRepository;

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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,  key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS, allEntries = true)
    })
    public ReviewResponse respondToReview(UUID reviewId, SellerReviewResponseRequest request, UUID sellerId) {
        log.info("Seller={} responding to review={}", sellerId, reviewId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        if (review.getSellerId() != null && !review.getSellerId().equals(sellerId)) {
            throw new ReviewAccessDeniedException("Review does not belong to this seller's products");
        }

        if (sellerReviewResponseRepository.existsByReview_Id(review.getId())) {
            throw new ReviewAlreadyExistsException("A response already exists for this review");
        }

        SellerReviewResponse response = SellerReviewResponse.builder()
                .review(review)
                .sellerId(sellerId)
                .body(request.getBody())
                .status(SellerResponseStatus.ACTIVE)
                .build();

        sellerReviewResponseRepository.save(response);
        log.info("Seller response created for review={}", reviewId);
        return toResponse(review);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,  key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS, allEntries = true)
    })
    public ReviewResponse updateResponse(UUID reviewId, SellerReviewResponseRequest request, UUID sellerId) {
        log.info("Seller={} updating response to review={}", sellerId, reviewId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        SellerReviewResponse existing = sellerReviewResponseRepository.findByReview_Id(review.getId())
                .orElseThrow(() -> new ReviewNotFoundException("No response found for review: " + reviewId));

        if (!existing.getSellerId().equals(sellerId)) {
            throw new ReviewAccessDeniedException("Response does not belong to this seller");
        }

        existing.setBody(request.getBody());
        sellerReviewResponseRepository.save(existing);
        log.info("Seller response updated for review={}", reviewId);
        return toResponse(review);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,  key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS, allEntries = true)
    })
    public ReviewResponse deleteResponse(UUID reviewId, UUID sellerId) {
        log.info("Seller={} deleting response to review={}", sellerId, reviewId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        SellerReviewResponse existing = sellerReviewResponseRepository.findByReview_Id(review.getId())
                .orElseThrow(() -> new ReviewNotFoundException("No response found for review: " + reviewId));

        if (!existing.getSellerId().equals(sellerId)) {
            throw new ReviewAccessDeniedException("Response does not belong to this seller");
        }

        existing.setStatus(SellerResponseStatus.DELETED);
        sellerReviewResponseRepository.save(existing);
        log.info("Seller response marked DELETED for review={}", reviewId);
        return toResponse(review);
    }
}

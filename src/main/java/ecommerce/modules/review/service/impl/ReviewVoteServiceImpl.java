package ecommerce.modules.review.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.review.dto.ReviewVoteRequest;
import ecommerce.modules.review.dto.ReviewVoteResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewVote;
import ecommerce.modules.review.enums.ReviewVoteType;
import ecommerce.modules.review.exception.ReviewAccessDeniedException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.repository.ReviewVoteRepository;
import ecommerce.modules.review.service.ReviewVoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewVoteServiceImpl implements ReviewVoteService {

    private final ReviewRepository reviewRepository;
    private final ReviewVoteRepository reviewVoteRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,   key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS,  allEntries = true)
    })
    public ReviewVoteResponse vote(UUID reviewId, ReviewVoteRequest request, UUID customerId) {
        log.info("Vote on review={} by customer={} voteType={}", reviewId, customerId, request.getVoteType());

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        if (review.getCustomerId().equals(customerId)) {
            throw new ReviewAccessDeniedException("You cannot vote on your own review");
        }

        Optional<ReviewVote> existingOpt = reviewVoteRepository.findByReview_IdAndCustomerId(review.getId(), customerId);

        if (existingOpt.isPresent()) {
            ReviewVote existing = existingOpt.get();
            existing.setVoteType(request.getVoteType());
            reviewVoteRepository.save(existing);
            log.debug("Updated existing vote for review={}", reviewId);
        } else {
            ReviewVote newVote = ReviewVote.builder()
                    .review(review)
                    .customerId(customerId)
                    .voteType(request.getVoteType())
                    .build();
            reviewVoteRepository.save(newVote);
            log.debug("Created new vote for review={}", reviewId);
        }

        long helpfulCount    = reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.HELPFUL);
        long notHelpfulCount = reviewVoteRepository.countByReview_IdAndVoteType(review.getId(), ReviewVoteType.NOT_HELPFUL);

        return ReviewVoteResponse.builder()
                .reviewId(reviewId)
                .voteType(request.getVoteType())
                .helpfulCount(helpfulCount)
                .notHelpfulCount(notHelpfulCount)
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.REVIEW,  key = "#reviewId"),
            @CacheEvict(value = CacheNames.REVIEWS, allEntries = true)
    })
    public void removeVote(UUID reviewId, UUID customerId) {
        log.info("Remove vote on review={} by customer={}", reviewId, customerId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        reviewVoteRepository.deleteByReview_IdAndCustomerId(review.getId(), customerId);
        log.debug("Vote removed for review={}", reviewId);
    }
}

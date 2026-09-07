package ecommerce.modules.review.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.common.exception.AuthorizationException;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.review.dto.*;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.review.spec.ReviewSpec;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SecurityService securityService;

    private static final String REVIEW_NOT_FOUND = "Review not found";

    // ─── Conversion helper ────────────────────────────────────────────────────

    private ReviewResponse toReviewResponse(Review review) {
        ReviewResponse.UserInfo userInfo = null;
        if (review.getCustomer() != null) {
            User c = review.getCustomer();
            userInfo = ReviewResponse.UserInfo.builder()
                    .id(c.getPublicId())
                    .firstName(c.getFirstName())
                    .lastName(c.getLastName())
                    .email(c.getEmail())
                    .build();
        }

        LocalDateTime createdAt = review.getCreatedAt() != null
                ? LocalDateTime.ofInstant(review.getCreatedAt(), ZoneId.systemDefault()) : null;
        LocalDateTime updatedAt = review.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(review.getUpdatedAt(), ZoneId.systemDefault()) : null;

        int helpful = review.getHelpful() != null ? review.getHelpful() : 0;
        int unhelpful = review.getUnhelpful() != null ? review.getUnhelpful() : 0;
        int total = helpful + unhelpful;
        double helpfulPct = total > 0 ? (double) helpful / total * 100.0 : 0.0;

        return ReviewResponse.builder()
                .id(review.getPublicId())
                .productId(review.getProduct() != null ? review.getProduct().getPublicId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .user(userInfo)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .approved(review.getApproved())
                .helpfulCount(helpful)
                .notHelpfulCount(unhelpful)
                .helpfulPercentage(helpfulPct)
                .totalVotes(total)
                .adminResponse(review.getAdminResponse())
                .adminResponseAt(review.getAdminResponseAt())
                .rejectionReason(review.getRejectionReason())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats",
            "rating-distribution", "review-trends", "top-rated-products", "most-reviewed-products"
    }, allEntries = true)
    public ReviewResponse createReview(ReviewCreateRequest request, UUID userId) {
        log.info("Creating review for product {} by user {}", request.getProductId(), userId);

        Product product = productRepository.findByPublicId(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        User customer = userRepository.findByPublicId(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));

        if (reviewRepository.existsByCustomer_PublicIdAndProduct_PublicId(userId, request.getProductId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        boolean hasOrdered = orderRepository.existsByCustomerIdAndProductId(customer.getId(), product.getId());

        Review review = Review.builder()
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .pros(request.getPros() != null ? String.join(",", request.getPros()) : null)
                .cons(request.getCons() != null ? String.join(",", request.getCons()) : null)
                .hasImages(request.getImages() != null && !request.getImages().isEmpty())
                .verifiedPurchase(hasOrdered)
                .approved(hasOrdered)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review created successfully with publicId: {}", saved.getPublicId());
        return toReviewResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats", "rating-distribution", "review-trends"
    }, allEntries = true)
    @CachePut(value = "review", key = "#reviewId")
    public ReviewResponse updateReview(UUID reviewId, ReviewUpdateRequest request, UUID userId) {
        log.info("Updating review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));

        if (!review.canBeEditedBy(userId)) {
            throw new AuthorizationException("You can only update your own reviews");
        }

        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getTitle() != null) review.setTitle(request.getTitle());
        if (request.getComment() != null) review.setComment(request.getComment());
        if (request.getPros() != null) review.setPros(String.join(",", request.getPros()));
        if (request.getCons() != null) review.setCons(String.join(",", request.getCons()));
        if (request.getImages() != null) review.setHasImages(!request.getImages().isEmpty());

        if (request.getRating() != null || request.getComment() != null) {
            review.setApproved(review.getVerifiedPurchase());
        }

        Review updated = reviewRepository.save(review);
        log.info("Review {} updated successfully", reviewId);
        return toReviewResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats", "rating-distribution",
            "review-trends", "top-rated-products", "most-reviewed-products"
    }, allEntries = true)
    public void deleteReview(UUID reviewId, UUID userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));

        if (!review.canBeDeletedBy(userId)) {
            throw new AuthorizationException("You can only delete your own reviews");
        }

        review.softDelete();
        reviewRepository.save(review);
        log.info("Review {} deleted successfully", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "review", key = "#reviewId")
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findByPublicIdWithUserAndProduct(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        return toReviewResponse(review);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats", "rating-distribution",
            "review-trends", "top-rated-products", "most-reviewed-products"
    }, allEntries = true)
    public ReviewResponse restoreReview(UUID reviewId, UUID userId) {
        log.info("Restoring review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));

        if (!review.getCustomer().getPublicId().equals(userId)) {
            throw new AuthorizationException("You can only restore your own reviews");
        }

        review.restore();
        Review restored = reviewRepository.save(review);
        log.info("Review {} restored successfully", reviewId);
        return toReviewResponse(restored);
    }

    // ─── Query operations ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'product:' + #productId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        log.debug("Fetching reviews for product {}", productId);

        if (productRepository.findByPublicId(productId).isEmpty()) {
            throw ResourceNotFoundException.forResource("Product", productId);
        }

        return reviewRepository.findByProduct_PublicIdAndApproved(productId, true, pageable)
                .map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'filtered-product:' + #productId + ':' + T(org.springframework.util.DigestUtils).md5DigestAsHex((#filters.toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort).getBytes())")
    public Page<ReviewResponse> getProductReviewsWithFilters(UUID productId, ReviewFilterRequest filters, Pageable pageable) {
        log.debug("Fetching filtered reviews for product {}", productId);

        Specification<Review> spec = Specification.where(ReviewSpec.hasProductPublicId(productId))
                .and(ReviewSpec.hasRating(filters.getRating()))
                .and(ReviewSpec.isVerifiedPurchase(filters.getVerifiedPurchase()))
                .and(ReviewSpec.isApproved(filters.getApproved()))
                .and(filters.getDateFrom() != null ? ReviewSpec.createdAfter(filters.getDateFrom().toInstant(java.time.ZoneOffset.UTC)) : null)
                .and(filters.getDateTo() != null ? ReviewSpec.createdBefore(filters.getDateTo().toInstant(java.time.ZoneOffset.UTC)) : null)
                .and(ReviewSpec.isActive());

        return reviewRepository.findAll(spec, pageable).map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'verified-product:' + #productId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ReviewResponse> getVerifiedReviews(UUID productId, Pageable pageable) {
        return reviewRepository.findByProduct_PublicIdAndVerifiedPurchase(productId, true, pageable)
                .map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user-reviews", key = "'user:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ReviewResponse> getUserReviews(UUID userId, Pageable pageable) {
        log.debug("Fetching reviews for user {}", userId);

        if (userRepository.findByPublicId(userId).isEmpty()) {
            throw ResourceNotFoundException.forResource("User", userId);
        }

        return reviewRepository.findByCustomer_PublicId(userId, pageable).map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'product-rating:' + #productId + ':' + #rating + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ReviewResponse> getReviewsByRating(UUID productId, Integer rating, Pageable pageable) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        return reviewRepository.findByProduct_PublicIdAndRating(productId, rating, pageable)
                .map(this::toReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "review-lists", key = "'most-helpful:' + #productId + ':' + #limit")
    public List<ReviewResponse> getMostHelpfulReviews(UUID productId, int limit) {
        Product product = productRepository.findByPublicId(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));
        return reviewRepository.findMostHelpfulReviews(product.getId(), limit)
                .stream().map(this::toReviewResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "review-lists", key = "'recent:' + #productId + ':' + #limit")
    public List<ReviewResponse> getRecentReviews(UUID productId, int limit) {
        Product product = productRepository.findByPublicId(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));
        return reviewRepository.findRecentReviews(product.getId(), limit)
                .stream().map(this::toReviewResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "'with-images:' + #productId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ReviewResponse> getReviewsWithImages(UUID productId, Pageable pageable) {
        return reviewRepository.findByHasImagesTrueAndIsActiveTrue(pageable).map(this::toReviewResponse);
    }

    // ─── Advanced querying ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews-predicate", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#spec=' + #spec.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<ReviewResponse> findReviewsWithPredicate(Specification<Review> spec, Pageable pageable) {
        return reviewRepository.findAll(spec, pageable).map(this::toReviewResponse);
    }

    // ─── Statistics ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "review-stats", key = "'product:' + #productId")
    public ReviewSummaryResponse getProductRatingStats(UUID productId) {
        log.debug("Fetching rating statistics for product {}", productId);

        Product product = productRepository.findByPublicId(productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", productId));

        Object[] stats = reviewRepository.getProductRatingStats(product.getId());
        List<Object[]> distribution = reviewRepository.getRatingDistributionWithPercentages(product.getId());
        List<String> topPros = reviewRepository.getMostCommonPros(product.getId(), 5);
        List<String> topCons = reviewRepository.getMostCommonCons(product.getId(), 5);

        long totalReviews = 0L;
        double avgRating = 0.0;
        long verifiedPurchases = 0L;
        if (stats != null) {
            if (stats.length > 0 && stats[0] instanceof Number n) totalReviews = n.longValue();
            if (stats.length > 1 && stats[1] instanceof Number n) avgRating = n.doubleValue();
            if (stats.length > 2 && stats[2] instanceof Number n) verifiedPurchases = n.longValue();
        }
        double verifiedPct = totalReviews > 0 ? (double) verifiedPurchases / totalReviews * 100.0 : 0.0;

        return ReviewSummaryResponse.builder()
                .totalReviews(totalReviews)
                .averageRating(avgRating)
                .verifiedPurchases(verifiedPurchases)
                .verifiedPurchasePercentage(verifiedPct)
                .distribution(buildRatingDistribution(distribution))
                .topPros(topPros)
                .topCons(topCons)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsResponse getAdminReviewStats() {
        Object[] stats = reviewRepository.getAdminReviewStats();
        long pending = reviewRepository.countPendingReviews();
        long approved = reviewRepository.countApprovedReviews();
        long rejected = reviewRepository.countRejectedReviews();

        long total = 0;
        double avgRating = 0.0;
        if (stats != null && stats.length >= 2) {
            if (stats[0] instanceof Number n) total = n.longValue();
            if (stats[1] instanceof Number n) avgRating = n.doubleValue();
        }

        return ReviewStatsResponse.builder()
                .totalReviews(total)
                .pendingReviews(pending)
                .approvedReviews(approved)
                .rejectedReviews(rejected)
                .averageRating(avgRating)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsResponse getSellerReviewStats(UUID sellerId) {
        User seller = userRepository.findByPublicId(sellerId).orElse(null);
        if (seller == null) {
            return ReviewStatsResponse.builder().totalReviews(0).averageRating(0.0).build();
        }
        Long sellerLongId = seller.getId();

        Object[] stats = reviewRepository.getSellerReviewStats(sellerLongId);
        long pending = reviewRepository.countPendingSellerReviews(sellerLongId);
        List<Object[]> distribution = reviewRepository.getSellerRatingDistribution(sellerLongId);

        long total = 0;
        double avgRating = 0.0;
        if (stats != null && stats.length >= 2) {
            if (stats[0] instanceof Number n) total = n.longValue();
            if (stats[1] instanceof Number n) avgRating = n.doubleValue();
        }

        Map<Integer, Long> ratingDist = new HashMap<>();
        long fiveStar = 0, fourStar = 0, threeStar = 0, twoStar = 0, oneStar = 0;
        if (distribution != null) {
            for (Object[] row : distribution) {
                Integer rating = (Integer) row[0];
                long count = ((Number) row[1]).longValue();
                ratingDist.put(rating, count);
                switch (rating) {
                    case 5 -> fiveStar = count;
                    case 4 -> fourStar = count;
                    case 3 -> threeStar = count;
                    case 2 -> twoStar = count;
                    case 1 -> oneStar = count;
                }
            }
        }

        return ReviewStatsResponse.builder()
                .totalReviews(total)
                .pendingReviews(pending)
                .averageRating(avgRating)
                .ratingDistribution(ratingDist)
                .fiveStarReviews(fiveStar)
                .fourStarReviews(fourStar)
                .threeStarReviews(threeStar)
                .twoStarReviews(twoStar)
                .oneStarReviews(oneStar)
                .build();
    }

    private ReviewSummaryResponse.RatingDistribution buildRatingDistribution(List<Object[]> distribution) {
        Map<Integer, Long> countMap = new HashMap<>();
        Map<Integer, Double> percentageMap = new HashMap<>();

        if (distribution != null) {
            for (Object[] row : distribution) {
                Integer rating = (Integer) row[0];
                countMap.put(rating, ((Number) row[1]).longValue());
                percentageMap.put(rating, (Double) row[2]);
            }
        }

        return ReviewSummaryResponse.RatingDistribution.builder()
                .fiveStars(countMap.getOrDefault(5, 0L))
                .fiveStarsPercentage(percentageMap.getOrDefault(5, 0.0))
                .fourStars(countMap.getOrDefault(4, 0L))
                .fourStarsPercentage(percentageMap.getOrDefault(4, 0.0))
                .threeStars(countMap.getOrDefault(3, 0L))
                .threeStarsPercentage(percentageMap.getOrDefault(3, 0.0))
                .twoStars(countMap.getOrDefault(2, 0L))
                .twoStarsPercentage(percentageMap.getOrDefault(2, 0.0))
                .oneStar(countMap.getOrDefault(1, 0L))
                .oneStarPercentage(percentageMap.getOrDefault(1, 0.0))
                .build();
    }

    // ─── Voting ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {"review", "reviews", "reviews-predicate", "review-lists"}, allEntries = true)
    public void markHelpful(UUID reviewId) {
        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.incrementHelpful();
        reviewRepository.save(review);
    }

    // ─── Admin operations ─────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats", "rating-distribution", "review-trends"
    }, allEntries = true)
    @CachePut(value = "review", key = "#reviewId")
    public ReviewResponse approveReview(UUID reviewId) {
        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.approve();
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats", "rating-distribution", "review-trends"
    }, allEntries = true)
    @CachePut(value = "review", key = "#reviewId")
    public ReviewResponse rejectReview(UUID reviewId, String reason) {
        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.reject(reason);
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"review", "reviews", "reviews-predicate"}, allEntries = true)
    public ReviewResponse addAdminResponse(UUID reviewId, AdminResponseRequest request) {
        UUID adminId = securityService.getCurrentUserId();
        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.addAdminResponse(request.getResponse(), adminId);
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"review", "reviews", "reviews-predicate"}, allEntries = true)
    public ReviewResponse removeAdminResponse(UUID reviewId) {
        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Review", reviewId));
        review.setAdminResponse(null);
        review.setAdminResponseAt(null);
        review.setAdminResponseBy(null);
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "reviews-predicate", "review-stats", "rating-distribution",
            "review-trends", "admin-reviews"
    }, allEntries = true)
    public int bulkApproveReviews(List<UUID> reviewIds) {
        log.info("Bulk approving {} reviews", reviewIds.size());
        return reviewRepository.approveReviews(reviewIds);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "product-reviews", "review-stats", "rating-distribution",
            "review-trends", "admin-reviews"
    }, allEntries = true)
    public int bulkRejectReviews(List<UUID> reviewIds, String reason) {
        log.info("Bulk rejecting {} reviews", reviewIds.size());
        return reviewRepository.rejectReviews(reviewIds, reason);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "reviews", "product-reviews", "review-stats", "rating-distribution",
            "review-trends", "admin-reviews"
    }, allEntries = true)
    public int bulkDeleteReviews(List<UUID> reviewIds) {
        int count = 0;
        for (UUID reviewId : reviewIds) {
            Review review = reviewRepository.findByPublicId(reviewId).orElse(null);
            if (review != null) {
                review.softDelete();
                reviewRepository.save(review);
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional
    public ReviewResponse sellerReply(UUID reviewId, UUID sellerId, String reply) {
        log.info("Seller {} replying to review {}", sellerId, reviewId);

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(REVIEW_NOT_FOUND));

        if (review.getProduct() == null || review.getProduct().getSeller() == null) {
            throw new BadRequestException("Review does not belong to a product");
        }

        if (!review.getProduct().getSeller().getPublicId().equals(sellerId)) {
            throw new AuthorizationException("You can only reply to reviews on your own products");
        }

        review.setSellerReply(reply);
        review.setSellerRepliedAt(LocalDateTime.now());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    public Page<ReviewResponse> getSellerReviews(UUID sellerId, Pageable pageable) {
        List<UUID> productIds = productRepository.findBySeller_PublicId(sellerId, Pageable.unpaged()).getContent()
                .stream().map(Product::getPublicId).collect(java.util.stream.Collectors.toList());
        if (productIds.isEmpty()) return Page.empty(pageable);
        return reviewRepository.findAll().stream()
                .filter(r -> productIds.contains(r.getProduct().getPublicId()))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> {
                            int start = (int) pageable.getOffset();
                            int end = Math.min(start + pageable.getPageSize(), list.size());
                            List<ReviewResponse> content = start < list.size()
                                    ? list.subList(start, end).stream().map(this::toReviewResponse).collect(java.util.stream.Collectors.toList())
                                    : java.util.Collections.emptyList();
                            return new org.springframework.data.domain.PageImpl<>(content, pageable, list.size());
                        }));
    }
}

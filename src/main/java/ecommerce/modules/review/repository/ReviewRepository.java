package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByPublicId(UUID publicId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    Page<Review> findByProductIdAndApproved(Long productId, Boolean approved, Pageable pageable);

    Page<Review> findByProductIdAndVerifiedPurchase(Long productId, Boolean verifiedPurchase, Pageable pageable);

    Page<Review> findByProductIdAndRating(Long productId, Integer rating, Pageable pageable);

    Page<Review> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.deleted = false ORDER BY r.helpful DESC")
    List<Review> findMostHelpfulReviews(@Param("productId") Long productId, @Param("limit") int limit);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(@Param("productId") Long productId, @Param("limit") int limit);

    @Query("SELECT r FROM Review r WHERE r.hasImages = true AND r.deleted = false")
    Page<Review> findByHasImagesTrueAndIsActiveTrue(Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.product WHERE r.id = :reviewId AND r.deleted = false")
    Optional<Review> findByIdWithUserAndProduct(@Param("reviewId") Long reviewId);

    @Query("SELECT COUNT(r), AVG(r.rating), SUM(CASE WHEN r.verifiedPurchase = true THEN 1 ELSE 0 END) FROM Review r WHERE r.product.id = :productId AND r.deleted = false")
    Object[] getProductRatingStats(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r), (COUNT(r) * 100.0 / (SELECT COUNT(*) FROM Review r2 WHERE r2.product.id = :productId AND r2.deleted = false)) FROM Review r WHERE r.product.id = :productId AND r.deleted = false GROUP BY r.rating")
    List<Object[]> getRatingDistributionWithPercentages(@Param("productId") Long productId);

    @Query("SELECT r.pros FROM Review r WHERE r.product.id = :productId AND r.pros IS NOT NULL AND r.deleted = false GROUP BY r.pros ORDER BY COUNT(r.pros) DESC")
    List<String> getMostCommonPros(@Param("productId") Long productId, int limit);

    @Query("SELECT r.cons FROM Review r WHERE r.product.id = :productId AND r.cons IS NOT NULL AND r.deleted = false GROUP BY r.cons ORDER BY COUNT(r.cons) DESC")
    List<String> getMostCommonCons(@Param("productId") Long productId, int limit);

    @Query("UPDATE Review r SET r.approved = true WHERE r.id IN :reviewIds")
    int approveReviews(List<Long> reviewIds);

    @Query("UPDATE Review r SET r.approved = false, r.rejectionReason = :reason WHERE r.id IN :reviewIds")
    int rejectReviews(List<Long> reviewIds, String reason);

    @Query("SELECT COUNT(r), AVG(r.rating) FROM Review r WHERE r.deleted = false")
    Object[] getAdminReviewStats();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.approved = false")
    long countPendingReviews();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.approved = true")
    long countApprovedReviews();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.approved = false")
    long countRejectedReviews();

    @Query("SELECT r.rating, COUNT(r) FROM Review r JOIN r.product p WHERE p.seller.id = :sellerId AND r.deleted = false GROUP BY r.rating")
    List<Object[]> getSellerRatingDistribution(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r), AVG(r.rating) FROM Review r JOIN r.product p WHERE p.seller.id = :sellerId AND r.deleted = false")
    Object[] getSellerReviewStats(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM Review r JOIN r.product p WHERE p.seller.id = :sellerId AND r.deleted = false AND r.approved = false")
    long countPendingSellerReviews(@Param("sellerId") Long sellerId);
}

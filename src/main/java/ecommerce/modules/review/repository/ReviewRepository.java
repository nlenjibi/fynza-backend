package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByPublicId(UUID publicId);

    boolean existsByCustomer_PublicIdAndProduct_Id(UUID customerPublicId, UUID productId);

    Page<Review> findByProduct_IdAndApproved(UUID productId, Boolean approved, Pageable pageable);

    Page<Review> findByProduct_IdAndVerifiedPurchase(UUID productId, Boolean verifiedPurchase, Pageable pageable);

    Page<Review> findByProduct_IdAndRating(UUID productId, Integer rating, Pageable pageable);

    Page<Review> findByCustomer_PublicId(UUID customerPublicId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.deleted = false ORDER BY r.helpful DESC")
    List<Review> findMostHelpfulReviews(@Param("productId") UUID productId, @Param("limit") int limit);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(@Param("productId") UUID productId, @Param("limit") int limit);

    @Query("SELECT r FROM Review r WHERE r.hasImages = true AND r.deleted = false")
    Page<Review> findByHasImagesTrueAndIsActiveTrue(Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.product WHERE r.publicId = :publicId AND r.deleted = false")
    Optional<Review> findByPublicIdWithUserAndProduct(@Param("publicId") UUID publicId);

    @Query("SELECT COUNT(r), AVG(r.rating), SUM(CASE WHEN r.verifiedPurchase = true THEN 1 ELSE 0 END) FROM Review r WHERE r.product.id = :productId AND r.deleted = false")
    Object[] getProductRatingStats(@Param("productId") UUID productId);

    @Query("SELECT r.rating, COUNT(r), (COUNT(r) * 100.0 / (SELECT COUNT(*) FROM Review r2 WHERE r2.product.id = :productId AND r2.deleted = false)) FROM Review r WHERE r.product.id = :productId AND r.deleted = false GROUP BY r.rating")
    List<Object[]> getRatingDistributionWithPercentages(@Param("productId") UUID productId);

    @Query("SELECT r.pros FROM Review r WHERE r.product.id = :productId AND r.pros IS NOT NULL AND r.deleted = false GROUP BY r.pros ORDER BY COUNT(r.pros) DESC")
    List<String> getMostCommonPros(@Param("productId") UUID productId, int limit);

    @Query("SELECT r.cons FROM Review r WHERE r.product.id = :productId AND r.cons IS NOT NULL AND r.deleted = false GROUP BY r.cons ORDER BY COUNT(r.cons) DESC")
    List<String> getMostCommonCons(@Param("productId") UUID productId, int limit);

    @Modifying
    @Query("UPDATE Review r SET r.approved = true WHERE r.publicId IN :publicIds")
    int approveReviews(@Param("publicIds") List<UUID> publicIds);

    @Modifying
    @Query("UPDATE Review r SET r.approved = false, r.rejectionReason = :reason WHERE r.publicId IN :publicIds")
    int rejectReviews(@Param("publicIds") List<UUID> publicIds, @Param("reason") String reason);

    @Query("SELECT COUNT(r), AVG(r.rating) FROM Review r WHERE r.deleted = false")
    Object[] getAdminReviewStats();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.approved = false")
    long countPendingReviews();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.approved = true")
    long countApprovedReviews();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.approved = false")
    long countRejectedReviews();

    // Seller-product association delegated to seller module — returns zeros until wired
    default List<Object[]> getSellerRatingDistribution(UUID sellerId) {
        return Collections.emptyList();
    }

    default Object[] getSellerReviewStats(UUID sellerId) {
        return new Object[]{0L, 0.0};
    }

    default long countPendingSellerReviews(UUID sellerId) {
        return 0L;
    }
}

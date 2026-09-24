package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByPublicId(UUID publicId);

    boolean existsByProductIdAndCustomerIdAndOrderItemId(UUID productId, UUID customerId, UUID orderItemId);

    boolean existsByProductIdAndCustomerId(UUID productId, UUID customerId);

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findBySellerIdAndStatus(UUID sellerId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStoreIdAndStatus(UUID storeId, ReviewStatus status, Pageable pageable);

    Page<Review> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    long countByProductIdAndStatus(UUID productId, ReviewStatus status);

    long countByStoreIdAndStatus(UUID storeId, ReviewStatus status);

    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = 'PUBLISHED'")
    List<Review> findPublishedByProductId(@Param("productId") UUID productId);

    @Query("SELECT r FROM Review r WHERE r.storeId = :storeId AND r.status = 'PUBLISHED'")
    List<Review> findPublishedByStoreId(@Param("storeId") UUID storeId);

    long countByStatusAndCreatedAtBefore(ReviewStatus status, Instant threshold);
}

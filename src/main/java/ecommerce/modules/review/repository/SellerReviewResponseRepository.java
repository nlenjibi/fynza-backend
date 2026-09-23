package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.SellerReviewResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerReviewResponseRepository extends JpaRepository<SellerReviewResponse, Long> {

    Optional<SellerReviewResponse> findByReview_Id(Long reviewId);

    Optional<SellerReviewResponse> findByPublicId(UUID publicId);

    boolean existsByReview_Id(Long reviewId);
}

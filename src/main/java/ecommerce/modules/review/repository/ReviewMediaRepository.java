package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, UUID> {

    List<ReviewMedia> findByReview_IdOrderBySortOrderAsc(UUID reviewId);

    void deleteByReview_IdAndPublicId(UUID reviewId, UUID publicId);

    long countByReview_Id(UUID reviewId);
}

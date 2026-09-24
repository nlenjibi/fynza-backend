package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewModeration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewModerationRepository extends JpaRepository<ReviewModeration, UUID> {

    List<ReviewModeration> findByReview_IdOrderByCreatedAtDesc(UUID reviewId);
}

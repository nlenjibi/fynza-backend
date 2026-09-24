package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewModeration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewModerationRepository extends JpaRepository<ReviewModeration, Long> {

    List<ReviewModeration> findByReview_IdOrderByCreatedAtDesc(Long reviewId);
}

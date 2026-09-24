package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewAuditLogRepository extends JpaRepository<ReviewAuditLog, Long> {

    List<ReviewAuditLog> findByReview_IdOrderByCreatedAtDesc(Long reviewId);
}

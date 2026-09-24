package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewAuditLogRepository extends JpaRepository<ReviewAuditLog, UUID> {

    List<ReviewAuditLog> findByReview_IdOrderByCreatedAtDesc(UUID reviewId);
}

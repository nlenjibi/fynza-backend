package ecommerce.modules.review.repository;

import ecommerce.modules.review.entity.ReviewReport;
import ecommerce.modules.review.enums.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    Optional<ReviewReport> findByPublicId(UUID publicId);

    boolean existsByReview_IdAndReporterId(Long reviewId, UUID reporterId);

    Page<ReviewReport> findByStatus(ReviewReportStatus status, Pageable pageable);

    List<ReviewReport> findByReview_Id(Long reviewId);
}

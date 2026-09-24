package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ResolveReportRequest;
import ecommerce.modules.review.dto.ReviewReportRequest;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.enums.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewReportService {

    ReviewReportResponse reportReview(UUID reviewId, ReviewReportRequest request, UUID reporterId);

    ReviewReportResponse resolveReport(UUID reportId, ResolveReportRequest request, UUID moderatorId);

    Page<ReviewReportResponse> getReports(ReviewReportStatus status, Pageable pageable);
}

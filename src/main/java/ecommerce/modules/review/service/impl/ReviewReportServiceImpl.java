package ecommerce.modules.review.service.impl;

import ecommerce.common.cache.CacheNames;
import ecommerce.modules.review.dto.ResolveReportRequest;
import ecommerce.modules.review.dto.ReviewReportRequest;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewReport;
import ecommerce.modules.review.enums.ReviewReportStatus;
import ecommerce.modules.review.exception.ReviewAlreadyExistsException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.ReviewReportRepository;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewReportServiceImpl implements ReviewReportService {

    private static final int FLAG_THRESHOLD = 3;

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;

    private ReviewReportResponse toResponse(ReviewReport report) {
        return ReviewReportResponse.builder()
                .id(report.getPublicId())
                .reviewId(report.getReview().getPublicId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.REVIEW, key = "#reviewId")
    public ReviewReportResponse reportReview(UUID reviewId, ReviewReportRequest request, UUID reporterId) {
        log.info("Report review={} by reporter={} reason={}", reviewId, reporterId, request.getReason());

        Review review = reviewRepository.findByPublicId(reviewId)
                .orElseThrow(() -> ReviewNotFoundException.forId(reviewId));

        if (reviewReportRepository.existsByReview_IdAndReporterId(review.getId(), reporterId)) {
            throw new ReviewAlreadyExistsException("You have already reported this review");
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporterId(reporterId)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReviewReportStatus.OPEN)
                .build();

        ReviewReport saved = reviewReportRepository.save(report);

        long reportCount = reviewReportRepository.findByReview_Id(review.getId()).size();
        if (reportCount >= FLAG_THRESHOLD && review.getStatus().isPubliclyVisible()) {
            review.markFlagged();
            reviewRepository.save(review);
            log.info("Review={} flagged after {} reports", reviewId, reportCount);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewReportResponse resolveReport(UUID reportId, ResolveReportRequest request, UUID moderatorId) {
        log.info("Resolve report={} by moderator={} resolution={}", reportId, moderatorId, request.getResolution());

        ReviewReport report = reviewReportRepository.findByPublicId(reportId)
                .orElseThrow(() -> new ReviewNotFoundException("Report not found: " + reportId));

        report.setStatus(request.getResolution());
        report.setResolvedBy(moderatorId);
        report.setResolvedAt(Instant.now());

        ReviewReport saved = reviewReportRepository.save(report);
        return toResponse(saved);
    }

    @Override
    public Page<ReviewReportResponse> getReports(ReviewReportStatus status, Pageable pageable) {
        return reviewReportRepository.findByStatus(status, pageable).map(this::toResponse);
    }
}

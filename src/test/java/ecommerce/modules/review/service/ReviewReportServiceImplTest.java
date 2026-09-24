package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ResolveReportRequest;
import ecommerce.modules.review.dto.ReviewReportRequest;
import ecommerce.modules.review.dto.ReviewReportResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.entity.ReviewReport;
import ecommerce.modules.review.enums.ReviewReportReason;
import ecommerce.modules.review.enums.ReviewReportStatus;
import ecommerce.modules.review.enums.ReviewStatus;
import ecommerce.modules.review.exception.ReviewAlreadyExistsException;
import ecommerce.modules.review.exception.ReviewNotFoundException;
import ecommerce.modules.review.repository.ReviewReportRepository;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.review.service.impl.ReviewReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewReportServiceImpl")
class ReviewReportServiceImplTest {

    @Mock private ReviewRepository       reviewRepository;
    @Mock private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private ReviewReportServiceImpl service;

    private UUID reviewPublicId;
    private UUID reportPublicId;
    private UUID reporterId;
    private UUID moderatorId;
    private Review review;

    @BeforeEach
    void setUp() {
        reviewPublicId  = UUID.randomUUID();
        reportPublicId  = UUID.randomUUID();
        reporterId      = UUID.randomUUID();
        moderatorId     = UUID.randomUUID();
        review          = buildReview(ReviewStatus.PUBLISHED);
    }

    // ── Builder helpers ───────────────────────────────────────────────────────────

    private Review buildReview(ReviewStatus status) {
        Review r = Review.builder()
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .rating(3)
                .status(status)
                .isActive(true)
                .build();
        try {
            java.lang.reflect.Field field = Review.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(r, 42L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    private ReviewReportRequest buildReportRequest() {
        return ReviewReportRequest.builder()
                .reason(ReviewReportReason.SPAM)
                .description("This is spam")
                .build();
    }

    private ReviewReport buildReport(Review targetReview, ReviewReportStatus status) {
        ReviewReport report = ReviewReport.builder()
                .review(targetReview)
                .reporterId(reporterId)
                .reason(ReviewReportReason.SPAM)
                .description("This is spam")
                .status(status)
                .build();
        try {
            java.lang.reflect.Field f = ReviewReport.class.getDeclaredField("publicId");
            f.setAccessible(true);
            f.set(report, reportPublicId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return report;
    }

    // ── reportReview ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reportReview")
    class ReportReview {

        @Test
        void reportReview_reviewNotFound_throwsNotFound() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reportReview(reviewPublicId, buildReportRequest(), reporterId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining(reviewPublicId.toString());

            verify(reviewReportRepository, never()).save(any());
        }

        @Test
        void reportReview_alreadyReported_throwsAlreadyExists() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(reviewReportRepository.existsByReview_IdAndReporterId(review.getId(), reporterId))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.reportReview(reviewPublicId, buildReportRequest(), reporterId))
                    .isInstanceOf(ReviewAlreadyExistsException.class)
                    .hasMessageContaining("already reported");

            verify(reviewReportRepository, never()).save(any());
        }

        @Test
        void reportReview_success_createsReport() {
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(review));
            when(reviewReportRepository.existsByReview_IdAndReporterId(review.getId(), reporterId))
                    .thenReturn(false);

            ArgumentCaptor<ReviewReport> reportCaptor = ArgumentCaptor.forClass(ReviewReport.class);
            when(reviewReportRepository.save(reportCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            // Fewer than FLAG_THRESHOLD existing reports → no flagging
            when(reviewReportRepository.findByReview_Id(review.getId())).thenReturn(List.of());

            ReviewReportResponse response = service.reportReview(reviewPublicId, buildReportRequest(), reporterId);

            ReviewReport saved = reportCaptor.getValue();
            assertThat(saved.getReason()).isEqualTo(ReviewReportReason.SPAM);
            assertThat(saved.getStatus()).isEqualTo(ReviewReportStatus.OPEN);
            assertThat(saved.getReporterId()).isEqualTo(reporterId);
            assertThat(response).isNotNull();
        }

        @Test
        void reportReview_thresholdReached_flagsReview() {
            // Review must be PUBLISHED (isPubliclyVisible == true) to be flaggable
            Review publishedReview = buildReview(ReviewStatus.PUBLISHED);
            when(reviewRepository.findByPublicId(reviewPublicId)).thenReturn(Optional.of(publishedReview));
            when(reviewReportRepository.existsByReview_IdAndReporterId(publishedReview.getId(), reporterId))
                    .thenReturn(false);

            ReviewReport saved = buildReport(publishedReview, ReviewReportStatus.OPEN);
            when(reviewReportRepository.save(any(ReviewReport.class))).thenReturn(saved);

            // Return 3 reports to hit the FLAG_THRESHOLD (>= 3)
            ReviewReport r1 = buildReport(publishedReview, ReviewReportStatus.OPEN);
            ReviewReport r2 = buildReport(publishedReview, ReviewReportStatus.OPEN);
            ReviewReport r3 = buildReport(publishedReview, ReviewReportStatus.OPEN);
            when(reviewReportRepository.findByReview_Id(publishedReview.getId()))
                    .thenReturn(List.of(r1, r2, r3));

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            when(reviewRepository.save(reviewCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            service.reportReview(reviewPublicId, buildReportRequest(), reporterId);

            assertThat(reviewCaptor.getValue().getStatus()).isEqualTo(ReviewStatus.FLAGGED);
            verify(reviewRepository).save(publishedReview);
        }
    }

    // ── resolveReport ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveReport")
    class ResolveReport {

        @Test
        void resolveReport_notFound_throwsNotFound() {
            when(reviewReportRepository.findByPublicId(reportPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolveReport(reportPublicId,
                    ResolveReportRequest.builder().resolution(ReviewReportStatus.RESOLVED).build(),
                    moderatorId))
                    .isInstanceOf(ReviewNotFoundException.class)
                    .hasMessageContaining("Report not found");
        }

        @Test
        void resolveReport_success_setsResolvedStatus() {
            ReviewReport report = buildReport(review, ReviewReportStatus.OPEN);
            when(reviewReportRepository.findByPublicId(reportPublicId)).thenReturn(Optional.of(report));

            ArgumentCaptor<ReviewReport> reportCaptor = ArgumentCaptor.forClass(ReviewReport.class);
            when(reviewReportRepository.save(reportCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            ReviewReportResponse response = service.resolveReport(reportPublicId,
                    ResolveReportRequest.builder().resolution(ReviewReportStatus.RESOLVED).build(),
                    moderatorId);

            ReviewReport saved = reportCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ReviewReportStatus.RESOLVED);
            assertThat(saved.getResolvedBy()).isEqualTo(moderatorId);
            assertThat(saved.getResolvedAt()).isNotNull();
            assertThat(response).isNotNull();
        }
    }
}

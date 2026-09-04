package ecommerce.modules.admin.async;

import ecommerce.common.config.AsyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private final AsyncProperties asyncProperties;

    @Async("reportExecutor")
    public CompletableFuture<ReportResult> generateReportAsync(UUID reportId, String reportType) {
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Starting async report generation for report: {}, type: {}", 
                correlationId, reportId, reportType);

        long timeout = asyncProperties.getTimeouts().getReport();

        return queryDataAsync(reportId)
                .thenComposeAsync(v -> aggregateDataAsync(reportId))
                .thenComposeAsync(v -> renderChartsAsync(reportId))
                .thenComposeAsync(v -> exportReportAsync(reportId, reportType))
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .thenApply(v -> ReportResult.builder()
                        .reportId(reportId)
                        .status(ReportResult.ReportStatus.COMPLETED)
                        .progressPercentage(100)
                        .completedAt(LocalDateTime.now())
                        .downloadUrl("/api/v1/admin/reports/" + reportId + "/download")
                        .build())
                .exceptionally(ex -> {
                    log.error("[{}] Report generation failed for report: {}", correlationId, reportId, ex);
                    return ReportResult.builder()
                            .reportId(reportId)
                            .status(ReportResult.ReportStatus.FAILED)
                            .errorMessage(ex.getMessage())
                            .build();
                });
    }

    private CompletableFuture<Void> queryDataAsync(UUID reportId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("[{}] Stage 1/4: Querying data for report: {}", reportId, reportId);
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            updateProgress(reportId, 25);
        });
    }

    private CompletableFuture<Void> aggregateDataAsync(UUID reportId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("[{}] Stage 2/4: Aggregating data for report: {}", reportId, reportId);
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            updateProgress(reportId, 50);
        });
    }

    private CompletableFuture<Void> renderChartsAsync(UUID reportId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("[{}] Stage 3/4: Rendering charts for report: {}", reportId, reportId);
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            updateProgress(reportId, 75);
        });
    }

    private CompletableFuture<Void> exportReportAsync(UUID reportId, String reportType) {
        return CompletableFuture.runAsync(() -> {
            log.debug("[{}] Stage 4/4: Exporting report: {} as {}", reportId, reportId, reportType);
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            updateProgress(reportId, 100);
        });
    }

    private void updateProgress(UUID reportId, int progress) {
        log.debug("Report {} progress: {}%", reportId, progress);
    }

    public ReportResult getReportStatus(UUID reportId) {
        return ReportResult.builder()
                .reportId(reportId)
                .status(ReportResult.ReportStatus.PROCESSING)
                .progressPercentage(50)
                .build();
    }
}

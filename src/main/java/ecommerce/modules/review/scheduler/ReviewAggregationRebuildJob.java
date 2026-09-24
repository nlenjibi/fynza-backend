package ecommerce.modules.review.scheduler;

import ecommerce.modules.review.service.ReviewAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewAggregationRebuildJob {

    private final ReviewAggregationService aggregationService;

    // Runs at 3 AM every Sunday to rebuild all aggregates from scratch
    @Scheduled(cron = "0 0 3 * * SUN")
    public void rebuildAllAggregates() {
        log.info("[ReviewAggregationRebuildJob] Starting full aggregate rebuild");
        try {
            aggregationService.rebuildAllAggregates();
            log.info("[ReviewAggregationRebuildJob] Aggregate rebuild completed");
        } catch (Exception e) {
            log.error("[ReviewAggregationRebuildJob] Aggregate rebuild failed: {}", e.getMessage(), e);
        }
    }
}

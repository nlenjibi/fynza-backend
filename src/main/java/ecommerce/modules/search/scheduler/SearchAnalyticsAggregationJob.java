package ecommerce.modules.search.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchAnalyticsAggregationJob {

    // Daily aggregation is handled implicitly by SearchAnalyticsService.trackSearch
    // which upserts per-query-per-day rows. This job exists as a hook for future
    // heavier aggregations (e.g., computing trending queries, populating suggestion caches).
    @Scheduled(cron = "0 0 1 * * *")
    public void aggregateAnalytics() {
        log.info("Daily search analytics aggregation tick");
    }
}

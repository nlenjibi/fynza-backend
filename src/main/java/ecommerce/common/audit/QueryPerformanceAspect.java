package ecommerce.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class QueryPerformanceAspect {

    private final Map<String, QueryStats> queryStats = new ConcurrentHashMap<>();
    private static final long SLOW_QUERY_THRESHOLD_MS = 500;

    public Report generateReport() {
        long totalQueries = queryStats.values().stream().mapToLong(QueryStats::getCount).sum();
        long totalSlowQueries = queryStats.values().stream().mapToLong(QueryStats::getSlowCount).sum();
        return new Report(totalQueries, totalSlowQueries);
    }

    public List<QueryEntry> getSlowestQueries(int limit) {
        return queryStats.entrySet().stream()
                .map(e -> new QueryEntry(e.getKey(), e.getValue()))
                .sorted((a, b) -> Double.compare(b.stats().getAverageTime(), a.stats().getAverageTime()))
                .limit(limit)
                .toList();
    }

    public record Report(long totalQueries, long totalSlowQueries) {
        public long getTotalQueries() { return totalQueries; }
        public long getTotalSlowQueries() { return totalSlowQueries; }
    }

    public record QueryEntry(String queryName, QueryStats stats) {}

    public static class QueryStats {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong slowCount = new AtomicLong();
        private volatile double totalTime;
        private volatile long maxTime;

        public void record(long durationMs) {
            count.incrementAndGet();
            totalTime += durationMs;
            if (durationMs > maxTime) maxTime = durationMs;
            if (durationMs > SLOW_QUERY_THRESHOLD_MS) slowCount.incrementAndGet();
        }

        public long getCount() { return count.get(); }
        public long getSlowCount() { return slowCount.get(); }
        public double getAverageTime() { return count.get() == 0 ? 0 : totalTime / count.get(); }
        public long getMaxTime() { return maxTime; }
    }
}

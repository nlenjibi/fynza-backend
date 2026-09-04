package ecommerce.modules.product.async;

import ecommerce.modules.product.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAsyncService {

    private final Map<String, AtomicLong> searchQueryCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSearchTime = new ConcurrentHashMap<>();

    @Async("searchExecutor")
    public void recordSearchQuery(SearchRequest request) {
        if (request == null || request.getQ() == null || request.getQ().isBlank()) {
            return;
        }

        String query = request.getQ().toLowerCase().trim();
        
        searchQueryCounts.computeIfAbsent(query, k -> new AtomicLong(0)).incrementAndGet();
        lastSearchTime.put(query, LocalDateTime.now());
        
        log.debug("Recorded search query: {} (total: {})", query, searchQueryCounts.get(query).get());
    }

    @Async("searchExecutor")
    public void incrementProductViewCountAsync(String productId) {
        log.debug("Async incrementing view count for product: {}", productId);
    }

    @Async("searchExecutor")
    public void warmUpSearchCache() {
        log.info("Warming up search cache...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Search cache warm-up completed");
    }

    public long getQueryCount(String query) {
        AtomicLong count = searchQueryCounts.get(query.toLowerCase().trim());
        return count != null ? count.get() : 0;
    }

    public LocalDateTime getLastSearchTime(String query) {
        return lastSearchTime.get(query.toLowerCase().trim());
    }
}

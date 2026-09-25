package ecommerce.modules.search.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexManager {

    private final AtomicReference<Instant> lastRebuiltAt = new AtomicReference<>();

    @Transactional(readOnly = true)
    public void indexProduct(UUID productId) {
        // PostgreSQL FTS is query-time — no explicit indexing needed.
        // The GIN index (082-product-search-gin-index.sql) keeps this fast.
        // This method is a hook for future Elasticsearch/OpenSearch integration.
        log.debug("Search index signalled for product {}", productId);
    }

    public void removeProduct(UUID productId) {
        // With PostgreSQL FTS, removing a product from the index means the product's
        // status/visibility already filters it out at query time (status = ACTIVE AND visibility = PUBLIC).
        // No additional action needed for PostgreSQL FTS.
        log.debug("Search index removal signalled for product {}", productId);
    }

    public Instant getLastRebuiltAt() {
        return lastRebuiltAt.get();
    }

    public void markRebuilt() {
        lastRebuiltAt.set(Instant.now());
    }
}

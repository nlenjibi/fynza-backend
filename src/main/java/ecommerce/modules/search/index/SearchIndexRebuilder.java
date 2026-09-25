package ecommerce.modules.search.index;

import ecommerce.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexRebuilder {

    private final ProductRepository   productRepository;
    private final SearchIndexManager  indexManager;

    private static final int BATCH_SIZE = 100;
    private final AtomicLong indexedCount = new AtomicLong(0);

    @Async
    @Transactional(readOnly = true)
    public void rebuild() {
        log.info("Search index rebuild started");
        indexedCount.set(0);
        int page = 0;

        while (true) {
            var products = productRepository.findAll(PageRequest.of(page, BATCH_SIZE));
            if (products.isEmpty()) break;
            products.forEach(p -> {
                indexManager.indexProduct(p.getId());
                indexedCount.incrementAndGet();
            });
            if (!products.hasNext()) break;
            page++;
        }

        indexManager.markRebuilt();
        log.info("Search index rebuild complete — {} products processed", indexedCount.get());
    }

    public long getLastIndexedCount() {
        return indexedCount.get();
    }
}

package ecommerce.modules.search.service;

import ecommerce.modules.search.dto.SearchIndexStatusResponse;
import ecommerce.modules.search.index.SearchIndexManager;
import ecommerce.modules.search.index.SearchIndexRebuilder;
import ecommerce.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {

    private final SearchIndexManager  indexManager;
    private final SearchIndexRebuilder rebuilder;
    private final ProductRepository   productRepository;

    public void triggerReindex() {
        log.info("Search reindex triggered");
        rebuilder.rebuild();
    }

    public void retryFailed() {
        log.debug("Search index retry — no failed documents in PostgreSQL FTS mode");
    }

    public SearchIndexStatusResponse getStatus() {
        long total = productRepository.count();
        return new SearchIndexStatusResponse(
                total,
                total,
                indexManager.getLastRebuiltAt(),
                "HEALTHY"
        );
    }
}

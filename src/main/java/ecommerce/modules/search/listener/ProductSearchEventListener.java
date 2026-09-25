package ecommerce.modules.search.listener;

import ecommerce.modules.product.event.ProductCreatedEvent;
import ecommerce.modules.product.event.ProductDeletedEvent;
import ecommerce.modules.product.event.ProductPublishedEvent;
import ecommerce.modules.product.event.ProductUnpublishedEvent;
import ecommerce.modules.product.event.ProductUpdatedEvent;
import ecommerce.modules.search.index.SearchIndexManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSearchEventListener {

    private final SearchIndexManager indexManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        indexManager.indexProduct(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUpdated(ProductUpdatedEvent event) {
        indexManager.indexProduct(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductPublished(ProductPublishedEvent event) {
        indexManager.indexProduct(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductUnpublished(ProductUnpublishedEvent event) {
        indexManager.removeProduct(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductDeleted(ProductDeletedEvent event) {
        indexManager.removeProduct(event.productId());
    }
}

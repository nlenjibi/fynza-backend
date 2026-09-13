package ecommerce.modules.pricing.cache;

import ecommerce.common.cache.CacheNames;
import ecommerce.common.cache.RedisCacheService;
import ecommerce.common.event.pricing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceCacheInvalidationListener {

    private final RedisCacheService cacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPriceActivated(PriceActivatedEvent event) {
        invalidateForProduct(event.productId().toString());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPriceDisabled(PriceDisabledEvent event) {
        invalidateForProduct(event.productId().toString());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPriceUpdated(PriceUpdatedEvent event) {
        invalidateForProduct(event.productId().toString());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPriceOverrideCreated(PriceOverrideCreatedEvent event) {
        invalidateForProduct(event.productId().toString());
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void invalidateForProduct(String productId) {
        String pattern = CacheNames.PRICE_EFFECTIVE + ":" + productId + ":*";
        cacheService.clear(pattern);
        log.debug("Invalidated price cache for product {}", productId);
    }
}

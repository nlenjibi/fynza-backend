package ecommerce.modules.search.listener;

import ecommerce.modules.review.enums.ReviewTargetType;
import ecommerce.modules.review.event.ReviewRatingAggregateUpdatedEvent;
import ecommerce.modules.search.index.SearchIndexManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewSearchEventListener {

    private final SearchIndexManager indexManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRatingUpdated(ReviewRatingAggregateUpdatedEvent event) {
        if (event.targetType() == ReviewTargetType.PRODUCT) {
            indexManager.indexProduct(event.targetId());
        }
    }
}

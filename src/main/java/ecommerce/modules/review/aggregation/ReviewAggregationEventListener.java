package ecommerce.modules.review.aggregation;

import ecommerce.modules.review.event.ReviewDeletedEvent;
import ecommerce.modules.review.event.ReviewPublishedEvent;
import ecommerce.modules.review.service.ReviewAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewAggregationEventListener {

    private final ReviewAggregationService aggregationService;

    @EventListener
    @Async
    public void onReviewPublished(ReviewPublishedEvent event) {
        log.debug("Updating aggregates for published review: {}", event.reviewId());
        if (event.productId() != null) {
            aggregationService.updateAggregateForProduct(event.productId());
        }
        if (event.storeId() != null) {
            aggregationService.updateAggregateForStore(event.storeId());
        }
    }

    @EventListener
    @Async
    public void onReviewDeleted(ReviewDeletedEvent event) {
        log.debug("Updating aggregates for deleted review: {}", event.reviewId());
        if (event.productId() != null) {
            aggregationService.updateAggregateForProduct(event.productId());
        }
        if (event.storeId() != null) {
            aggregationService.updateAggregateForStore(event.storeId());
        }
    }
}

package ecommerce.modules.review.service;

import ecommerce.modules.review.dto.ReviewSummaryResponse;
import ecommerce.modules.review.enums.ReviewTargetType;

import java.util.UUID;

public interface ReviewAggregationService {

    void updateAggregateForProduct(UUID productId);

    void updateAggregateForStore(UUID storeId);

    ReviewSummaryResponse getAggregate(ReviewTargetType targetType, UUID targetId);

    void rebuildAllAggregates();
}

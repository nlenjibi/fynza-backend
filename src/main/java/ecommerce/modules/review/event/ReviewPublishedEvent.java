package ecommerce.modules.review.event;

import java.util.UUID;

public record ReviewPublishedEvent(UUID reviewId, UUID customerId, UUID productId, UUID storeId, Integer rating, boolean verifiedPurchase) {}

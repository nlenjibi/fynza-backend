package ecommerce.modules.review.event;

import java.util.UUID;

public record ReviewDeletedEvent(UUID reviewId, UUID productId, UUID storeId) {}

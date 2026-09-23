package ecommerce.modules.review.event;

import java.util.UUID;

public record ReviewCreatedEvent(UUID reviewId, UUID customerId, UUID productId, UUID storeId, UUID sellerId, Integer rating) {}

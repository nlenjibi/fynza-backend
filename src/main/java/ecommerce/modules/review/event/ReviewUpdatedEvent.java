package ecommerce.modules.review.event;

import java.util.UUID;

public record ReviewUpdatedEvent(UUID reviewId, UUID customerId, UUID productId) {}

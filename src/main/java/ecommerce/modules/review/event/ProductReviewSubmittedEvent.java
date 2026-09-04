package ecommerce.modules.review.event;

import java.util.UUID;

public record ProductReviewSubmittedEvent(
    UUID reviewId,
    UUID productId,
    String productName,
    UUID customerId,
    String customerEmail,
    UUID sellerId,
    int rating
) {}

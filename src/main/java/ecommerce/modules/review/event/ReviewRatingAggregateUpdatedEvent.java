package ecommerce.modules.review.event;

import ecommerce.modules.review.enums.ReviewTargetType;

import java.util.UUID;

public record ReviewRatingAggregateUpdatedEvent(ReviewTargetType targetType, UUID targetId) {}

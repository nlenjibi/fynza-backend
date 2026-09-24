package ecommerce.modules.review.policy;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Component
public class ReviewEligibilityPolicy {

    private static final int REVIEW_WINDOW_DAYS = 90;

    private static final Set<OrderStatus> ELIGIBLE_STATUSES = Set.of(
            OrderStatus.DELIVERED,
            OrderStatus.CONFIRMED,
            OrderStatus.RETURNED,
            OrderStatus.REFUNDED
    );

    public boolean isOrderEligibleForReview(Order order) {
        return order != null && ELIGIBLE_STATUSES.contains(order.getStatus());
    }

    public boolean isWithinReviewWindow(Order order) {
        if (order.getCreatedAt() == null) {
            return true;
        }
        return Instant.now().isBefore(order.getCreatedAt().plus(REVIEW_WINDOW_DAYS, ChronoUnit.DAYS));
    }

    public boolean isOwner(Order order, UUID customerId) {
        return customerId.equals(order.getCustomerId());
    }

    public boolean orderContainsItem(Order order, UUID orderItemPublicId) {
        return order.getOrderItems().stream()
                .anyMatch(item -> orderItemPublicId.equals(item.getPublicId()));
    }
}

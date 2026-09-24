package ecommerce.modules.review.policy;

import ecommerce.modules.review.entity.Review;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class ReviewEditPolicy {

    private static final int EDIT_WINDOW_DAYS = 30;

    public boolean canEdit(Review review, UUID userId) {
        if (!review.canBeEditedBy(userId)) {
            return false;
        }
        if (review.getPublishedAt() == null) {
            return true;
        }
        return Instant.now().isBefore(review.getPublishedAt().plus(EDIT_WINDOW_DAYS, ChronoUnit.DAYS));
    }
}

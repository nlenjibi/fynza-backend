package ecommerce.modules.review.policy;

import ecommerce.modules.review.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewModerationPolicy {

    /**
     * Returns true if the review must enter the moderation queue before being published.
     * Default behaviour: all reviews require moderation.
     */
    public boolean requiresModeration(Review review) {
        return true;
    }
}

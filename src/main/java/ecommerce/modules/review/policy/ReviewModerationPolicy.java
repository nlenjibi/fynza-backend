package ecommerce.modules.review.policy;

import org.springframework.stereotype.Component;

@Component
public class ReviewModerationPolicy {

    public boolean requiresModeration() {
        return true;
    }
}

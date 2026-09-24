package ecommerce.modules.review.exception;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(String message) {
        super(message);
    }

    public static ReviewNotFoundException forId(UUID id) {
        return new ReviewNotFoundException("Review not found: " + id);
    }
}

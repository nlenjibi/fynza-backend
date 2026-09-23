package ecommerce.modules.review.exception;

public class ReviewAccessDeniedException extends RuntimeException {

    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}

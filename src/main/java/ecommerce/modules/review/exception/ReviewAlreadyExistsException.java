package ecommerce.modules.review.exception;

public class ReviewAlreadyExistsException extends RuntimeException {

    public ReviewAlreadyExistsException(String message) {
        super(message);
    }
}

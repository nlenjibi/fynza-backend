package ecommerce.modules.refund.exception;

public class ReturnNotFoundException extends RuntimeException {

    public ReturnNotFoundException(String message) {
        super(message);
    }
}

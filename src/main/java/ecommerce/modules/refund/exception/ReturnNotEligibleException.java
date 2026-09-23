package ecommerce.modules.refund.exception;

public class ReturnNotEligibleException extends RuntimeException {

    public ReturnNotEligibleException(String message) {
        super(message);
    }
}

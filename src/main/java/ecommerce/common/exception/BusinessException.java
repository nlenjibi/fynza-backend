package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated (e.g. coupon already used, order not cancellable).
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class BusinessException extends FynzaException {

    public BusinessException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessException(String message, String code) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, code);
    }
}

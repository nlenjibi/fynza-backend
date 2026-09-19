package ecommerce.modules.order.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends FynzaException {
    public InvalidOrderStateException(String message) {
        super(message, HttpStatus.CONFLICT, "INVALID_ORDER_STATE");
    }
}

package ecommerce.modules.order.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends FynzaException {
    public OrderNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND");
    }
}

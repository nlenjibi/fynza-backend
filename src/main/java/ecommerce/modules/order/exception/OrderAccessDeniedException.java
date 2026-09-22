package ecommerce.modules.order.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class OrderAccessDeniedException extends FynzaException {
    public OrderAccessDeniedException() {
        super("You do not have access to this order", HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED");
    }
}

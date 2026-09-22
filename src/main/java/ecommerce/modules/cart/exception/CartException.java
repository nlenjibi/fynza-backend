package ecommerce.modules.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class CartException extends RuntimeException {

    private final CartErrorCode errorCode;

    public CartException(CartErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CartErrorCode getErrorCode() {
        return errorCode;
    }
}

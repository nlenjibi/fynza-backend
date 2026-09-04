package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class CartNotFoundException extends FynzaException {

    public CartNotFoundException() {
        super("Cart not found", HttpStatus.NOT_FOUND);
    }

    public CartNotFoundException(Long cartId) {
        super("Cart not found with ID: " + cartId, HttpStatus.NOT_FOUND);
    }

    public CartNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

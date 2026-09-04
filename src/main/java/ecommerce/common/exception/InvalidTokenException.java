package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends FynzaException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }
}

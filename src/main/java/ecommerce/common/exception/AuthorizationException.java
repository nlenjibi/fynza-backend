package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationException extends FynzaException {

    public AuthorizationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}

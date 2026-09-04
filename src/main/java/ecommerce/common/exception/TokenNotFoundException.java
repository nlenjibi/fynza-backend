package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class TokenNotFoundException extends FynzaException {

    public TokenNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "TOKEN_NOT_FOUND");
    }
}

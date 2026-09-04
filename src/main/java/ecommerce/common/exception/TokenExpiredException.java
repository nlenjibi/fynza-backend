package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends FynzaException {

    public TokenExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
    }
}

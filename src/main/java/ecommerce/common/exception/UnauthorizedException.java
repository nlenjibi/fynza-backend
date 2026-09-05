package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends FynzaException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    protected UnauthorizedException(String message, String code) {
        super(message, HttpStatus.UNAUTHORIZED, code);
    }
}

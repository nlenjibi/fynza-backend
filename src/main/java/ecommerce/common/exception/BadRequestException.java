package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends FynzaException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message, String code) {
        super(message, HttpStatus.BAD_REQUEST, code);
    }
}

package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends FynzaException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public ConflictException(String message, String code) {
        super(message, HttpStatus.CONFLICT, code);
    }
}

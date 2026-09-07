package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends FynzaException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String message, String code) {
        super(message, HttpStatus.CONFLICT, code);
    }
}

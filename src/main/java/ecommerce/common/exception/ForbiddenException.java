package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends FynzaException {

    public ForbiddenException() {
        super("You do not have permission to access this resource.", HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message, String code) {
        super(message, HttpStatus.FORBIDDEN, code);
    }

    public static ForbiddenException forMissingRole(String role) {
        return new ForbiddenException("Access denied. Required role: " + role);
    }
}

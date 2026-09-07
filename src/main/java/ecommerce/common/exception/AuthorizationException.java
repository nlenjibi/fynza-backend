package ecommerce.common.exception;

public class AuthorizationException extends ForbiddenException {

    public AuthorizationException(String message) {
        super(message);
    }
}

package ecommerce.common.exception;

public class TokenNotFoundException extends ResourceNotFoundException {

    public TokenNotFoundException(String message) {
        super(message, "TOKEN_NOT_FOUND");
    }
}

package ecommerce.common.exception;

public class TokenExpiredException extends UnauthorizedException {

    public TokenExpiredException(String message) {
        super(message, "TOKEN_EXPIRED");
    }
}

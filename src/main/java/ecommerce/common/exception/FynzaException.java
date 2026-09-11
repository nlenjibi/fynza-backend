package ecommerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Abstract base for all domain-specific exceptions in Fynza.
 * Every subclass carries an HTTP status and an optional machine-readable error code
 * that is serialised into the response body by {@link GlobalExceptionHandler}.
 */
@Getter
public abstract class FynzaException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected FynzaException(String message, HttpStatus status) {
        this(message, status, null);
    }

    protected FynzaException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

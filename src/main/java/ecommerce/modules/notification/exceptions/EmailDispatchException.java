package ecommerce.modules.notification.exceptions;

import lombok.Getter;

/**
 * Thrown when an EmailProvider fails to send an email.
 * The retryable flag distinguishes transient failures (SMTP timeouts) from
 * permanent failures (invalid recipient) that should not be retried.
 */
@Getter
public class EmailDispatchException extends RuntimeException {

    private final String providerName;
    private final boolean retryable;

    public EmailDispatchException(String providerName, String message,
                                  Throwable cause, boolean retryable) {
        super(message, cause);
        this.providerName = providerName;
        this.retryable    = retryable;
    }
}

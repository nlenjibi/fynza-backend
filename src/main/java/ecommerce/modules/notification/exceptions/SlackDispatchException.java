package ecommerce.modules.notification.exceptions;

import lombok.Getter;

/**
 * Thrown when the Slack client fails to deliver a message.
 * The retryable flag distinguishes transient failures from permanent ones.
 */
@Getter
public class SlackDispatchException extends RuntimeException {

    private final boolean retryable;

    public SlackDispatchException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }
}

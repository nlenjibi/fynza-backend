package ecommerce.modules.notification.provider;

import ecommerce.modules.notification.dto.EmailRequest;

/**
 * Strategy interface for sending outbound emails through an external provider.
 * Implementations throw EmailDispatchException on failure, indicating whether the error is retryable.
 */
public interface EmailProvider {

    /** Returns the unique provider name used in dispatch audit records (e.g. "GMAIL_SMTP"). */
    String providerName();

    /** Sends the email and returns a provider-assigned message ID. */
    String send(EmailRequest request);
}

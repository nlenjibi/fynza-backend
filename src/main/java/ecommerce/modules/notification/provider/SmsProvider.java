package ecommerce.modules.notification.provider;

public interface SmsProvider {

    /**
     * Sends an SMS to the given phone number.
     *
     * @return provider-assigned message ID, or null if unavailable
     */
    String send(String recipientPhone, String body, String idempotencyKey);

    /** Returns true when the given provider failure code is transient and safe to retry. */
    boolean isRetryable(String failureCode);

    String providerName();
}

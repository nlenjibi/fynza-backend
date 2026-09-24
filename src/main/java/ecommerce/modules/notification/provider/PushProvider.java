package ecommerce.modules.notification.provider;

import java.util.Map;

public interface PushProvider {

    /**
     * Sends a push notification to a single device token.
     *
     * @param deviceToken  FCM/APNs device token
     * @param title        notification title
     * @param body         notification body
     * @param data         optional key/value payload delivered alongside the notification
     * @return provider-assigned message ID, or null if unavailable
     */
    String send(String deviceToken, String title, String body, Map<String, String> data);

    /** Returns true when the given provider failure code is transient and safe to retry. */
    boolean isRetryable(String failureCode);

    /** Returns true when the token is permanently invalid and should be deactivated. */
    boolean isInvalidToken(String failureCode);

    String providerName();
}

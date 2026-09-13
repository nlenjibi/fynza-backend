package ecommerce.modules.notification.enums;

/**
 * Lifecycle states of a {@link ecommerce.modules.notification.entity.NotificationDispatch} record.
 * Transitions: PENDING → SENDING → SENT or FAILED.
 * Failed retryable dispatches revert to PENDING until max retries are exhausted.
 */
public enum NotificationStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED
}

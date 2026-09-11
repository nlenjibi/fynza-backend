package ecommerce.modules.notification.service;

import ecommerce.modules.notification.dto.EntityRef;
import ecommerce.modules.notification.dto.NotificationResponse;
import ecommerce.modules.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * Service contract for creating, dispatching, and querying notifications.
 * Evaluates channel config and per-user preferences before dispatching.
 */
public interface NotificationService {

    /**
     * Sends a notification of the given type to the recipient across all enabled channels.
     * Template {{placeholders}} are resolved from the variables map.
     *
     * @param type        the notification type
     * @param recipientId the user UUID of the intended recipient
     * @param sellerId    optional seller context (may be null)
     * @param variables   key/value pairs for template placeholder resolution
     * @param deepLink    optional URL for more context
     * @param entity      optional domain entity reference
     */
    void send(NotificationType type,
              UUID recipientId,
              UUID sellerId,
              Map<String, String> variables,
              String deepLink,
              EntityRef entity);

    /**
     * Sends an email-only notification to an external recipient with no user account.
     * Still tracks the attempt in notification_dispatch for retry support.
     */
    void sendToExternalRecipient(NotificationType type,
                                 String recipientEmail,
                                 Map<String, String> variables);

    /**
     * Sends a broadcast Slack message to the global or seller-specific channel.
     * A dedup guard makes a second call for the same (type, sourceEntityId) a safe no-op.
     */
    void sendBroadcast(NotificationType type,
                       UUID sourceEntityId,
                       UUID sellerId,
                       Map<String, String> variables);

    /** Returns a paginated list of in-app notifications for the recipient, newest first. */
    Page<NotificationResponse> getForRecipient(UUID recipientId, Pageable pageable);

    /** Returns the unread in-app notification count for the recipient. */
    long countUnread(UUID recipientId);

    /** Marks a single notification as read, scoped to its owning recipient. */
    void markAsRead(UUID publicId, UUID recipientId);

    /** Marks all unread notifications for the recipient as read. */
    void markAllAsRead(UUID recipientId);

    /** Soft-deletes a single notification, scoped to its owning recipient. */
    void softDelete(UUID publicId, UUID recipientId);

    /** Soft-deletes all notifications for the recipient. */
    void softDeleteAll(UUID recipientId);

    /** Returns a single notification by its public UUID, scoped to the recipient. */
    NotificationResponse getById(UUID publicId, UUID recipientId);
}

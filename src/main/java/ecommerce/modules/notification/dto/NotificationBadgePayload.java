package ecommerce.modules.notification.dto;

import lombok.Value;

/** Pushed to /user/{userId}/queue/notifications/badge on every read/delete/create mutation. */
@Value
public class NotificationBadgePayload {
    long unreadCount;
}

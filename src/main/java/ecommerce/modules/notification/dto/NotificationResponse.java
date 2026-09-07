package ecommerce.modules.notification.dto;

import ecommerce.modules.notification.entity.Notification;
import ecommerce.modules.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/** Read-only response DTO representing a single in-app notification. */
@Value
@Builder
public class NotificationResponse {

    UUID             publicId;
    NotificationType notificationType;
    String           title;
    String           body;
    String           deepLink;
    String           entityType;
    UUID             entityId;
    boolean          isRead;
    Instant          readAt;
    Instant          createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .publicId(n.getPublicId())
                .notificationType(n.getNotificationType())
                .title(n.getTitle())
                .body(n.getBody())
                .deepLink(n.getDeepLink())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .isRead(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

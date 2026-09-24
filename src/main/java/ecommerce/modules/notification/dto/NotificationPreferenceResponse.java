package ecommerce.modules.notification.dto;

import ecommerce.modules.notification.entity.NotificationPreference;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class NotificationPreferenceResponse {

    UUID    publicId;
    String  notificationType;
    String  channel;
    boolean enabled;
    Instant updatedAt;

    public static NotificationPreferenceResponse from(NotificationPreference p) {
        return NotificationPreferenceResponse.builder()
                .publicId(p.getPublicId())
                .notificationType(p.getNotificationType().name())
                .channel(p.getChannel().name())
                .enabled(p.isEnabled())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

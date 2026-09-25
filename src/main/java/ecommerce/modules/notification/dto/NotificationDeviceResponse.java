package ecommerce.modules.notification.dto;

import ecommerce.modules.notification.entity.NotificationDevice;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class NotificationDeviceResponse {

    UUID    publicId;
    String  platform;
    String  status;
    String  appVersion;
    Instant lastSeenAt;
    Instant createdAt;

    public static NotificationDeviceResponse from(NotificationDevice d) {
        return NotificationDeviceResponse.builder()
                .publicId(d.getPublicId())
                .platform(d.getPlatform().name())
                .status(d.getStatus().name())
                .appVersion(d.getAppVersion())
                .lastSeenAt(d.getLastSeenAt())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

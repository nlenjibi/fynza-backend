package ecommerce.modules.notification.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    private UUID notificationId;
    private String channel;
    private boolean success;
    private String message;
    private int retryCount;

    public static NotificationResult success(String channel) {
        return NotificationResult.builder()
                .notificationId(UUID.randomUUID())
                .channel(channel)
                .success(true)
                .message("Notification sent successfully")
                .retryCount(0)
                .build();
    }

    public static NotificationResult failure(String channel, String message, int retryCount) {
        return NotificationResult.builder()
                .notificationId(UUID.randomUUID())
                .channel(channel)
                .success(false)
                .message(message)
                .retryCount(retryCount)
                .build();
    }
}

package ecommerce.modules.notification.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private UUID relatedEntityId;
    private LocalDateTime createdAt;
}

package ecommerce.modules.subscriber.dto;

import ecommerce.modules.subscriber.entity.Subscriber;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberResponse {
    private UUID id;
    private String email;
    private Subscriber.SubscriberStatus status;
    private LocalDateTime subscribedAt;
    private LocalDateTime unsubscribedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubscriberResponse from(Subscriber subscriber) {
        return SubscriberResponse.builder()
                .id(subscriber.getId())
                .email(subscriber.getEmail())
                .status(subscriber.getStatus())
                .subscribedAt(subscriber.getSubscribedAt())
                .unsubscribedAt(subscriber.getUnsubscribedAt())
                .createdAt(subscriber.getCreatedAt())
                .updatedAt(subscriber.getUpdatedAt())
                .build();
    }
}

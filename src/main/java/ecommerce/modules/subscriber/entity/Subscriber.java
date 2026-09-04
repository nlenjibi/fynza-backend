package ecommerce.modules.subscriber.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscribers", indexes = {
    @Index(name = "idx_subscriber_email", columnList = "email", unique = true),
    @Index(name = "idx_subscriber_status", columnList = "status"),
    @Index(name = "idx_subscriber_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Subscriber extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubscriberStatus status = SubscriberStatus.ACTIVE;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    public enum SubscriberStatus {
        ACTIVE,
        UNSUBSCRIBED
    }
}

package ecommerce.modules.shipping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipping_webhook_events", indexes = {
        @Index(name = "idx_shipping_webhook_events_provider", columnList = "provider"),
        @Index(name = "idx_shipping_webhook_events_processed", columnList = "processed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ShippingWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, length = 200)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
    }
}

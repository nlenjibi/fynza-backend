package ecommerce.modules.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_settings", indexes = {
        @Index(name = "idx_store_settings_store_id", columnList = "store_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "GHS";

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Africa/Accra";

    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "order_notifications", nullable = false)
    @Builder.Default
    private Boolean orderNotifications = true;

    @Column(name = "customer_notifications", nullable = false)
    @Builder.Default
    private Boolean customerNotifications = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

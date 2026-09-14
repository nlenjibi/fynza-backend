package ecommerce.modules.media.entity;

import ecommerce.modules.media.enums.MediaOwnerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storage_usage", indexes = {
        @Index(name = "idx_storage_usage_owner", columnList = "owner_id, owner_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorageUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private MediaOwnerType ownerType;

    @Column(name = "storage_bytes", nullable = false)
    @Builder.Default
    private Long storageBytes = 0L;

    @Column(name = "object_count", nullable = false)
    @Builder.Default
    private Long objectCount = 0L;

    @Column(name = "bandwidth_bytes", nullable = false)
    @Builder.Default
    private Long bandwidthBytes = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = Instant.now();
    }
}

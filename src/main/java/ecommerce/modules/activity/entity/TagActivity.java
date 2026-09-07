package ecommerce.modules.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tag_activities", indexes = {
    @Index(name = "idx_tag_activity_user", columnList = "user_id"),
    @Index(name = "idx_tag_activity_tag", columnList = "tag_id"),
    @Index(name = "idx_tag_activity_type", columnList = "activity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tag_id")
    private UUID tagId;

    @Column(name = "product_id")
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "tag_name", length = 100)
    private String tagName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        publicId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ActivityType {
        TAG_CREATED,
        TAG_UPDATED,
        TAG_DELETED,
        TAG_ASSIGNED,
        TAG_UNASSIGNED,
        TAG_BULK_ASSIGNED,
        TAG_BULK_UNASSIGNED,
        TAG_VIEWED,
        TAG_SEARCHED
    }
}

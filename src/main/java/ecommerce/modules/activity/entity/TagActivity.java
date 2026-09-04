package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
public class TagActivity extends BaseEntity {

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

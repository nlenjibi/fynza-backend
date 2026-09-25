package ecommerce.modules.review.entity;

import ecommerce.modules.review.enums.ReviewMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "review")
public class ReviewMedia {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "id", insertable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "media_reference", length = 1000)
    private String mediaReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 30)
    private ReviewMediaType mediaType;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
    }
}

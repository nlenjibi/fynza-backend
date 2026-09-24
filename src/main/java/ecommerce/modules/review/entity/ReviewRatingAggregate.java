package ecommerce.modules.review.entity;

import ecommerce.modules.review.enums.ReviewTargetType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "review_rating_aggregates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rating_aggregate_target",
                        columnNames = {"target_type", "target_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRatingAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ReviewTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "average_rating", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "rating1_count", nullable = false)
    @Builder.Default
    private Integer rating1Count = 0;

    @Column(name = "rating2_count", nullable = false)
    @Builder.Default
    private Integer rating2Count = 0;

    @Column(name = "rating3_count", nullable = false)
    @Builder.Default
    private Integer rating3Count = 0;

    @Column(name = "rating4_count", nullable = false)
    @Builder.Default
    private Integer rating4Count = 0;

    @Column(name = "rating5_count", nullable = false)
    @Builder.Default
    private Integer rating5Count = 0;

    @Column(name = "verified_count", nullable = false)
    @Builder.Default
    private Integer verifiedCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = Instant.now();
    }

    public void recalculateAverage() {
        if (reviewCount == null || reviewCount == 0) {
            averageRating = BigDecimal.ZERO;
            return;
        }
        int sum = (coalesce(rating1Count) * 1)
                + (coalesce(rating2Count) * 2)
                + (coalesce(rating3Count) * 3)
                + (coalesce(rating4Count) * 4)
                + (coalesce(rating5Count) * 5);
        averageRating = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
    }

    private int coalesce(Integer value) {
        return value == null ? 0 : value;
    }
}

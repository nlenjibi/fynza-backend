package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {

    private UUID targetId;
    private ReviewTargetType targetType;
    private Long reviewCount;
    private BigDecimal averageRating;
    private Long rating1Count;
    private Long rating2Count;
    private Long rating3Count;
    private Long rating4Count;
    private Long rating5Count;
    private Long verifiedCount;
    private Double verifiedPercentage;
}

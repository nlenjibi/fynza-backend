package ecommerce.modules.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {
    private long totalReviews;
    private long pendingReviews;
    private long approvedReviews;
    private long rejectedReviews;
    private Double averageRating;
    private Map<Integer, Long> ratingDistribution;
    private long fiveStarReviews;
    private long fourStarReviews;
    private long threeStarReviews;
    private long twoStarReviews;
    private long oneStarReviews;
}

package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewVoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVoteResponse {

    private UUID reviewId;
    private ReviewVoteType voteType;
    private Long helpfulCount;
    private Long notHelpfulCount;
}

package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewVoteType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVoteRequest {

    @NotNull
    private ReviewVoteType voteType;
}

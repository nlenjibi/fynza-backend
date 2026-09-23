package ecommerce.modules.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEligibilityResponse {

    private boolean eligible;
    private String reason;
    private UUID existingReviewId;
    private boolean productEligible;
    private boolean storeEligible;
}

package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewModerationAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerateReviewRequest {

    @NotNull
    private ReviewModerationAction action;

    private String reasonCode;

    private String notes;
}

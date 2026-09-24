package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportRequest {

    @NotNull
    private ReviewReportReason reason;

    @Size(max = 1000)
    private String description;
}

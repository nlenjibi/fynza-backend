package ecommerce.modules.review.dto;

import ecommerce.modules.review.enums.ReviewReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReportRequest {

    @NotNull
    private ReviewReportStatus resolution;

    private String notes;
}

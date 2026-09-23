package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnResolution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteInspectionRequest {

    @NotNull(message = "Resolution is required")
    private ReturnResolution resolution;

    private String overallNotes;

    @NotEmpty(message = "At least one item result is required")
    @Valid
    private List<InspectionItemResult> itemResults;
}

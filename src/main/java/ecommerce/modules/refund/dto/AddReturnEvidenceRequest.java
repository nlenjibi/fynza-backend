package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnEvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddReturnEvidenceRequest {

    private UUID returnItemId;

    @NotBlank(message = "Media reference is required")
    private String mediaReference;

    @NotNull(message = "Evidence type is required")
    private ReturnEvidenceType evidenceType;

    private String description;
}

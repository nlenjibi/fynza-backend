package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.InspectionCondition;
import ecommerce.modules.refund.enums.InspectionResult;
import ecommerce.modules.refund.enums.ReturnDispositionType;
import jakarta.validation.constraints.Min;
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
public class InspectionItemResult {

    @NotNull(message = "Return item ID is required")
    private UUID returnItemId;

    @NotNull(message = "Inspection condition is required")
    private InspectionCondition condition;

    @NotNull(message = "Inspection result is required")
    private InspectionResult result;

    @Min(value = 0, message = "Approved quantity must be non-negative")
    private Integer approvedQuantity;

    @Min(value = 0, message = "Received quantity must be non-negative")
    private Integer receivedQuantity;

    private ReturnDispositionType dispositionType;

    @Min(value = 1, message = "Disposition quantity must be at least 1")
    private Integer dispositionQuantity;

    private String locationId;

    private String notes;
}

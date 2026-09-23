package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.InspectionCondition;
import ecommerce.modules.refund.enums.InspectionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnInspectionResponse {

    private UUID publicId;
    private UUID returnId;
    private UUID returnItemId;
    private UUID inspectedBy;
    private InspectionCondition condition;
    private InspectionResult result;
    private String notes;
    private Instant inspectedAt;
    private Instant createdAt;
}

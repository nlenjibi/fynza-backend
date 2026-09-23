package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnEvidenceType;
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
public class ReturnEvidenceResponse {

    private UUID publicId;
    private UUID returnId;
    private UUID returnItemId;
    private String mediaReference;
    private ReturnEvidenceType evidenceType;
    private String description;
    private UUID uploadedBy;
    private Instant createdAt;
}

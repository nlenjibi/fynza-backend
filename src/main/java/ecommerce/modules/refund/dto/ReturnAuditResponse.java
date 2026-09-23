package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnStatus;
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
public class ReturnAuditResponse {

    private Long id;
    private UUID returnId;
    private ReturnAuditAction action;
    private ReturnStatus previousStatus;
    private ReturnStatus newStatus;
    private UUID performedBy;
    private String reason;
    private Instant createdAt;
}

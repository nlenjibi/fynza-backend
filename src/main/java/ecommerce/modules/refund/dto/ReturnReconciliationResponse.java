package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReconciliationResult;
import ecommerce.modules.refund.enums.RefundStatus;
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
public class ReturnReconciliationResponse {

    private Long id;
    private UUID returnId;
    private ReturnStatus returnStatus;
    private String shipmentStatus;
    private RefundStatus refundStatus;
    private String inventoryStatus;
    private ReconciliationResult result;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private Instant createdAt;
}

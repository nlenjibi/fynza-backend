package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {

    private UUID publicId;
    private String returnNumber;
    private UUID orderId;
    private UUID customerId;
    private UUID sellerId;
    private UUID storeId;
    private ReturnStatus status;
    private ReturnReason reason;
    private String customerNote;
    private String adminNote;
    private String rejectionReason;
    private Instant returnDeadline;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant receivedAt;
    private Instant rejectedAt;
    private Instant resolvedAt;
    private Boolean isEscalated;
    private Instant escalatedAt;
    private List<ReturnItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}

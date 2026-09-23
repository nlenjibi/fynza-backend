package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRefundResponse {

    private UUID publicId;
    private UUID returnId;
    private UUID orderId;
    private UUID paymentId;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private String currency;
    private String reason;
    private RefundStatus status;
    private String providerRefundId;
    private String providerReference;
    private String failureReason;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant processedAt;
    private Instant createdAt;
}

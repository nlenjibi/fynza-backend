package ecommerce.modules.refund.dto;

import ecommerce.common.enums.RefundReason;
import ecommerce.common.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private UUID id;
    private String refundNumber;
    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private UUID sellerId;
    private String sellerName;
    private BigDecimal amount;
    private RefundStatus status;
    private RefundReason reason;
    private String reasonDisplayName;
    private String customerNote;
    private String adminNote;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
    private LocalDateTime completedAt;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

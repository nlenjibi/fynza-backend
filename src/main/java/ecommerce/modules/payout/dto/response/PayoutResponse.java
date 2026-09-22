package ecommerce.modules.payout.dto.response;

import ecommerce.modules.payout.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PayoutResponse {
    private UUID id;
    private String payoutNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private String currency;
    private PayoutStatus status;
    private String providerReference;
    private String failureReason;
    private Integer retryCount;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant completedAt;
}

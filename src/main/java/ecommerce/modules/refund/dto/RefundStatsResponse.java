package ecommerce.modules.refund.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatsResponse {

    private long totalRefunds;
    private long pending;
    private long approved;
    private long rejected;
    private long completed;
    private BigDecimal totalAmount;
    private BigDecimal pendingAmount;
    private BigDecimal approvedAmount;
    private BigDecimal rejectedAmount;
    private BigDecimal completedAmount;
    private Map<String, Long> byStatus;
    private Map<String, BigDecimal> amountByStatus;
}

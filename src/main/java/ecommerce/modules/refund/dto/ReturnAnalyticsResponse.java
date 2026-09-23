package ecommerce.modules.refund.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnAnalyticsResponse {

    private long totalReturns;
    private long resolvedReturns;
    private long pendingReturns;
    private long failedReturns;
    private List<ReturnStatusCount> countByStatus;
    private List<ReturnReasonCount> countByReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnStatusCount {
        private String status;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnReasonCount {
        private String reason;
        private long count;
    }
}

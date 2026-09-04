package ecommerce.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for order dashboard statistics.
 * Used by admin dashboard to display order-related metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDashboardDto {

    private Long totalOrders;
    private Long pendingOrders;
    private Map<String, Long> ordersByStatus;
    private List<RecentOrderDto> recentOrders;
    private Double orderCompletionRate;
    private Double cancellationRate;
    private BigDecimal totalRevenue;

    /**
     * DTO for recent order information in dashboard.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderDto {
        private String orderId;
        private String orderNumber;
        private String customerName;
        private String sellerName;
        private BigDecimal amount;
        private String status;
        private String createdAt;
    }
}

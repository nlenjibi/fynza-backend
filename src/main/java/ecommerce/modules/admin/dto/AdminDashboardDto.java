package ecommerce.modules.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdminDashboardDto {
    private Long totalUsers;
    private Long totalCustomers;
    private Long totalSellers;
    private Long totalOrders;
    private Long totalProducts;
    private BigDecimal totalRevenue;
    private Long pendingOrders;
    private Long activeUsers;
    private Long lowStockProducts;
    
    private Map<String, Long> ordersByStatus;
    private List<RecentOrderDto> recentOrders;
    private List<TopSellerDto> topSellers;
    private List<LowStockAlertDto> lowStockAlerts;
    
    private Double orderCompletionRate;
    private Double cancellationRate;
    
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
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellerDto {
        private String sellerId;
        private String sellerName;
        private Long productCount;
        private BigDecimal revenue;
        private Long orderCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockAlertDto {
        private String productId;
        private String productName;
        private String sku;
        private String sellerName;
        private Integer currentStock;
    }
}

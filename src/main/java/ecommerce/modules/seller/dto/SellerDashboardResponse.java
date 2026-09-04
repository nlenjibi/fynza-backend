package ecommerce.modules.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDashboardResponse {

    private Long totalProducts;
    private Long activeProducts;
    private Long totalOrders;
    private Long ordersThisMonth;
    private Long pendingOrders;
    private Long completedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal revenueGrowth;
    private Double averageRating;
    private Long totalCustomers;
    private Long storeVisits;
    private Double visitGrowth;
    private List<RecentOrderDto> recentOrders;
    private List<TopProductDto> topProducts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderDto {
        private String orderId;
        private String orderNumber;
        private String customerName;
        private String productName;
        private BigDecimal amount;
        private String status;
        private String timeAgo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDto {
        private String productName;
        private Long salesCount;
        private BigDecimal revenue;
        private Double growth;
    }
}

package ecommerce.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for seller-specific order dashboard and analytics.
 * Contains order-related data specific to a seller's perspective.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrderDto {

    private Long totalOrders;
    private Long ordersThisMonth;
    private Long lastMonthOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal lastMonthRevenue;
    private BigDecimal revenueGrowth;
    private Long totalCustomers;
    private List<RecentSellerOrderDto> recentOrders;

    /**
     * DTO for recent order from seller perspective.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSellerOrderDto {
        private String orderId;
        private String orderNumber;
        private String customerName;
        private String productName;
        private BigDecimal amount;
        private String status;
        private String timeAgo;
    }

    /**
     * DTO for order analytics from seller perspective.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerOrderAnalytics {
        private BigDecimal totalSales;
        private BigDecimal averageOrderValue;
        private Long totalOrders;
        private Long totalProductsSold;
        private Double conversionRate;
        private List<DailySalesDto> dailySales;
        private List<TopSellerProductDto> topProducts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySalesDto {
        private String date;
        private BigDecimal sales;
        private Long orders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellerProductDto {
        private String productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }
}

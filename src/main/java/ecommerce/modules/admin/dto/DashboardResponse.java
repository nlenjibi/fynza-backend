package ecommerce.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DashboardResponse
 * 
 * DTOs for returning aggregated analytics data to the frontend.
 * Contains dashboard metrics for admins, sellers, and customers.
 */
public class DashboardResponse {

    // ==================== Admin Dashboard ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboard {
        private long totalUsers;
        private long totalOrders;
        private BigDecimal totalRevenue;
        private BigDecimal averageOrderValue;
        private long activeSellers;
        private long activeCustomers;
        private List<TrendData> orderTrends;
        private List<TrendData> revenueTrends;
        private List<SellerPerformance> topSellers;
        private SystemHealth systemHealth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerPerformance {
        private String sellerId;
        private String sellerName;
        private long orderCount;
        private BigDecimal revenue;
        private double rating;
        private double cancellationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealth {
        private int healthyServices;
        private int degradedServices;
        private int downServices;
        private Map<String, String> serviceStatus;
    }

    // ==================== Seller Dashboard ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerDashboard {
        private long totalProducts;
        private long totalOrders;
        private BigDecimal totalRevenue;
        private double averageOrderValue;
        private double cancellationRate;
        private InventoryHealth inventoryHealth;
        private List<ProductPerformance> topProducts;
        private List<OrderTrendData> recentOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryHealth {
        private long totalProducts;
        private long lowStockCount;
        private long outOfStockCount;
        private List<ProductStockAlert> lowStockAlerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockAlert {
        private String productId;
        private String productName;
        private int currentStock;
        private int minimumStock;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPerformance {
        private String productId;
        private String productName;
        private long salesCount;
        private BigDecimal revenue;
        private double rating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTrendData {
        private LocalDateTime date;
        private long orderCount;
        private BigDecimal revenue;
    }

    // ==================== Customer Dashboard ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDashboard {
        private BigDecimal totalSpent;
        private long totalOrders;
        private double averageOrderValue;
        private List<CategoryPreference> categoryPreferences;
        private List<RecentPurchase> recentPurchases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryPreference {
        private String categoryId;
        private String categoryName;
        private long purchaseCount;
        private BigDecimal totalSpent;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentPurchase {
        private String orderId;
        private LocalDateTime purchaseDate;
        private BigDecimal total;
        private int itemCount;
        private String status;
    }

    // ==================== Common ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private LocalDateTime period;
        private long count;
        private BigDecimal value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private List<String> labels;
        private List<Long> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRangeResponse {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String period; // daily, weekly, monthly
    }
}

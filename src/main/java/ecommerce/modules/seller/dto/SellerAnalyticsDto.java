package ecommerce.modules.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive DTO for seller analytics dashboard.
 * Contains all metrics displayed in the analytics UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAnalyticsDto {

    // =================================================================
    // KEY METRICS
    // =================================================================
    
    private BigDecimal totalRevenue;
    private Double revenueGrowth;
    
    private Long totalOrders;
    private Double ordersGrowth;
    
    private Long totalCustomers;
    private Double customersGrowth;
    
    private BigDecimal averageOrderValue;
    private Double avgOrderValueGrowth;
    
    private Double conversionRate;
    private Double conversionRateGrowth;
    
    private Double refundRate;
    private Double refundRateGrowth;

    // =================================================================
    // CHARTS DATA
    // =================================================================
    
    private List<DailyMetric> dailyOrders;
    private List<MonthlyMetric> monthlyRevenue;
    private List<CategorySales> salesByCategory;
    private List<TopProductMetric> topProducts;
    private List<TopCustomerMetric> topCustomers;

    // =================================================================
    // NESTED DTOs
    // =================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetric {
        private String day;
        private Long orders;
        private BigDecimal revenue;
        private Long customers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyMetric {
        private String month;
        private BigDecimal revenue;
        private Long orders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySales {
        private String category;
        private Long sales;
        private BigDecimal revenue;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductMetric {
        private String productId;
        private String productName;
        private Long salesCount;
        private BigDecimal revenue;
        private Double growth;
        private Double rating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomerMetric {
        private String customerId;
        private String customerName;
        private Long totalOrders;
        private BigDecimal totalSpent;
        private String lastOrder;
    }
}

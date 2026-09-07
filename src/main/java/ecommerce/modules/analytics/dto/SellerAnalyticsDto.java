package ecommerce.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAnalyticsDto {

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

    private List<DailyMetric> dailyOrders;
    private List<MonthlyMetric> monthlyRevenue;
    private List<CategorySales> salesByCategory;
    private List<TopProductMetric> topProducts;
    private List<TopCustomerMetric> topCustomers;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyMetric {
        private String day;
        private Long orders;
        private BigDecimal revenue;
        private Long customers;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyMetric {
        private String month;
        private BigDecimal revenue;
        private Long orders;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategorySales {
        private String category;
        private Long sales;
        private BigDecimal revenue;
        private Double percentage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopProductMetric {
        private String productId;
        private String productName;
        private Long salesCount;
        private BigDecimal revenue;
        private Double growth;
        private Double rating;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopCustomerMetric {
        private String customerId;
        private String customerName;
        private Long totalOrders;
        private BigDecimal totalSpent;
        private String lastOrder;
    }
}

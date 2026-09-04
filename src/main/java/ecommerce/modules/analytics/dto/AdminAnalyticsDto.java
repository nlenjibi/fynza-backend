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
public class AdminAnalyticsDto {

    private String filterPeriod;
    private String startDate;
    private String endDate;

    private BigDecimal totalRevenue;
    private Double revenueGrowth;
    private Long totalOrders;
    private Double ordersGrowth;
    private Long totalCustomers;
    private Double customersGrowth;
    private Long totalSellers;
    private Double sellersGrowth;
    private Long productsSold;
    private Double productsSoldGrowth;
    private BigDecimal averageOrderValue;
    private Double avgOrderValueGrowth;

    private RevenueOverview revenueOverview;
    private List<SalesByCategory> salesByCategory;
    private List<TopSellerMetric> topSellers;
    private List<TopProductMetric> topProducts;
    private List<MonthlySalesTrend> monthlyTrend;
    private List<PaymentMethodBreakdown> paymentMethodBreakdown;
    private CommissionMetrics commissionMetrics;
    private SellerPayoutMetrics sellerPayoutMetrics;
    private NetRevenueMetrics netRevenueMetrics;
    private BigDecimal todayRevenue;
    private BigDecimal pendingPayments;
    private BigDecimal refundsProcessed;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RevenueOverview {
        private List<MonthlyRevenue> monthly;
        private BigDecimal total;
        private Double growth;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
        private Long orders;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SalesByCategory {
        private String category;
        private BigDecimal revenue;
        private Long sales;
        private Double percentage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopSellerMetric {
        private Integer rank;
        private String sellerId;
        private String sellerName;
        private Long orders;
        private BigDecimal revenue;
        private Double growth;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopProductMetric {
        private Integer rank;
        private String productId;
        private String productName;
        private Long salesCount;
        private BigDecimal revenue;
        private Double growth;
        private Double rating;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlySalesTrend {
        private String month;
        private BigDecimal revenue;
        private Long orders;
        private BigDecimal avgOrderValue;
        private Double growth;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentMethodBreakdown {
        private String method;
        private String displayName;
        private Long transactions;
        private BigDecimal amount;
        private Double share;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CommissionMetrics {
        private BigDecimal totalCommission;
        private Double growth;
        private BigDecimal platformCommission;
        private BigDecimal transactionFees;
        private BigDecimal paymentProcessing;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SellerPayoutMetrics {
        private BigDecimal total;
        private Double growth;
        private BigDecimal pending;
        private BigDecimal processing;
        private BigDecimal completed;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NetRevenueMetrics {
        private BigDecimal netRevenue;
        private Double growth;
        private BigDecimal grossRevenue;
        private BigDecimal refunds;
        private BigDecimal payouts;
    }
}

package ecommerce.modules.analytics.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-cutting analytics queries used by the admin dashboard and seller dashboards.
 * Implementations query order/product/user repositories directly — no separate
 * analytics tables are required for the initial phase.
 */
public interface AnalyticsService {

    long getTotalUsers();

    BigDecimal getTotalRevenue();

    BigDecimal getRevenueForPeriod(LocalDateTime start, LocalDateTime end);

    long getTotalOrderCount();

    long getOrderCountForPeriod(LocalDateTime start, LocalDateTime end);

    List<SellerMetrics> getTopSellers(int limit);

    List<TrendData> getOrderTrends(LocalDateTime start, LocalDateTime end);

    List<TrendData> getRevenueTrends(LocalDateTime start, LocalDateTime end);

    long getProductSales(UUID sellerId);

    BigDecimal getSellerRevenue(UUID sellerId);

    BigDecimal getSellerRevenueForPeriod(UUID sellerId, LocalDateTime start, LocalDateTime end);

    double getSellerCancellationRate(UUID sellerId);

    long getLowStockCount(UUID sellerId);

    SellerMetrics getSellerMetrics(UUID sellerId);

    BigDecimal getCustomerTotalSpending(UUID customerId);

    List<CategoryPreference> getCustomerCategoryPreferences(UUID customerId);

    void recordEvent(String eventType, UUID entityId, Map<String, Object> metadata);

    void refreshAnalyticsCache();

    record SellerMetrics(
            UUID sellerId,
            String sellerName,
            long totalOrders,
            BigDecimal totalRevenue,
            double cancellationRate,
            long lowStockCount,
            double averageRating
    ) {}

    record TrendData(
            LocalDateTime period,
            long count,
            BigDecimal value
    ) {}

    record CategoryPreference(
            UUID categoryId,
            String categoryName,
            long purchaseCount,
            BigDecimal totalSpent,
            double percentage
    ) {}
}

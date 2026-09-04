package ecommerce.modules.admin.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AnalyticsService Interface
 * 
 * Provides background collection of dashboard metrics for administrators and sellers.
 * Tracks analytics for orders, revenue, inventory, and user behavior.
 */
public interface AnalyticsService {

    // ==================== Admin Analytics ====================

    /**
     * Get total number of users
     */
    long getTotalUsers();

    /**
     * Get total revenue
     */
    BigDecimal getTotalRevenue();

    /**
     * Get revenue for a specific period
     */
    BigDecimal getRevenueForPeriod(LocalDateTime start, LocalDateTime end);

    /**
     * Get order count
     */
    long getTotalOrderCount();

    /**
     * Get order count for a specific period
     */
    long getOrderCountForPeriod(LocalDateTime start, LocalDateTime end);

    /**
     * Get top sellers by revenue
     */
    java.util.List<SellerMetrics> getTopSellers(int limit);

    /**
     * Get order trends (daily/weekly/monthly)
     */
    java.util.List<TrendData> getOrderTrends(LocalDateTime start, LocalDateTime end);

    /**
     * Get revenue trends
     */
    java.util.List<TrendData> getRevenueTrends(LocalDateTime start, LocalDateTime end);

    // ==================== Seller Analytics ====================

    /**
     * Get product sales for a seller
     */
    long getProductSales(UUID sellerId);

    /**
     * Get revenue for a seller
     */
    BigDecimal getSellerRevenue(UUID sellerId);

    /**
     * Get revenue for a seller in a specific period
     */
    BigDecimal getSellerRevenueForPeriod(UUID sellerId, LocalDateTime start, LocalDateTime end);

    /**
     * Get cancellation rate for a seller
     */
    double getSellerCancellationRate(UUID sellerId);

    /**
     * Get inventory health for a seller (low stock count)
     */
    long getLowStockCount(UUID sellerId);

    /**
     * Get seller metrics summary
     */
    SellerMetrics getSellerMetrics(UUID sellerId);

    // ==================== Customer Analytics ====================

    /**
     * Get total spending for a customer
     */
    BigDecimal getCustomerTotalSpending(UUID customerId);

    /**
     * Get customer category preferences
     */
    java.util.List<CategoryPreference> getCustomerCategoryPreferences(UUID customerId);

    // ==================== Event Recording ====================

    /**
     * Record an analytics event
     * @param eventType The type of event (ORDER_CREATED, ORDER_CONFIRMED, etc.)
     * @param entityId The ID of the entity related to the event
     * @param metadata Additional event metadata
     */
    void recordEvent(String eventType, UUID entityId, java.util.Map<String, Object> metadata);

    /**
     * Refresh analytics cache after significant data changes
     */
    void refreshAnalyticsCache();

    // ==================== Data Classes ====================

    /**
     * Seller metrics data class
     */
    record SellerMetrics(
        UUID sellerId,
        String sellerName,
        long totalOrders,
        BigDecimal totalRevenue,
        double cancellationRate,
        long lowStockCount,
        double averageRating
    ) {}

    /**
     * Trend data for charts
     */
    record TrendData(
        LocalDateTime period,
        long count,
        BigDecimal value
    ) {}

    /**
     * Category preference data
     */
    record CategoryPreference(
        UUID categoryId,
        String categoryName,
        long purchaseCount,
        BigDecimal totalSpent,
        double percentage
    ) {}
}

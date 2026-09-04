package ecommerce.graphql.resolver.analytics;

import ecommerce.graphql.dto.AdminStats;
import ecommerce.graphql.dto.PerformanceMetrics;
import ecommerce.modules.admin.dto.AdminAnalyticsDto;
import ecommerce.modules.admin.dto.ContentAnalyticsDto;
import ecommerce.modules.admin.service.AdminService;
import ecommerce.modules.admin.service.AnalyticsService;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AnalyticsResolver {

    private final AnalyticsService analyticsService;
    private final AdminService adminService;
    private final UserService userService;
    private final ProductService productService;

    // =========================================================================
    // ADMIN ANALYTICS
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAnalyticsDto adminAnalytics(@Argument String filter) {
        log.info("GQL adminAnalytics(filter={})", filter);
        return adminService.getAnalytics(filter != null ? filter : "month");
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminStats adminStats() {
        log.info("GQL adminStats");
        Map<String, Object> customerStats = userService.getCustomerStats();
        Map<String, Object> sellerStats = userService.getSellerStats();
        return AdminStats.builder()
                .totalUsers((Long) customerStats.getOrDefault("totalCustomers", 0L))
                .totalRevenue(analyticsService.getTotalRevenue())
                .totalOrderCount(analyticsService.getTotalOrderCount())
                .totalProducts(productService.getAdminProductStats().getTotalProducts())
                .totalSellers((Long) sellerStats.getOrDefault("totalSellers", 0L))
                .totalCustomers((Long) customerStats.getOrDefault("totalCustomers", 0L))
                .build();
    }

    // =========================================================================
    // INDIVIDUAL ADMIN METRICS
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Long totalUsers() {
        log.info("GQL totalUsers");
        return analyticsService.getTotalUsers();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal totalRevenue() {
        log.info("GQL totalRevenue");
        return analyticsService.getTotalRevenue();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal revenueForPeriod(@Argument LocalDateTime start, @Argument LocalDateTime end) {
        log.info("GQL revenueForPeriod({}, {})", start, end);
        return analyticsService.getRevenueForPeriod(start, end);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Long totalOrderCount() {
        log.info("GQL totalOrderCount");
        return analyticsService.getTotalOrderCount();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Long orderCountForPeriod(@Argument LocalDateTime start, @Argument LocalDateTime end) {
        log.info("GQL orderCountForPeriod({}, {})", start, end);
        return analyticsService.getOrderCountForPeriod(start, end);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnalyticsService.SellerMetrics> topSellers(@Argument int limit) {
        log.info("GQL topSellers(limit={})", limit);
        return analyticsService.getTopSellers(limit);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnalyticsService.TrendData> orderTrends(@Argument LocalDateTime start, @Argument LocalDateTime end) {
        log.info("GQL orderTrends({}, {})", start, end);
        return analyticsService.getOrderTrends(start, end);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnalyticsService.TrendData> revenueTrends(@Argument LocalDateTime start, @Argument LocalDateTime end) {
        log.info("GQL revenueTrends({}, {})", start, end);
        return analyticsService.getRevenueTrends(start, end);
    }

    // =========================================================================
    // CONTENT ANALYTICS
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ContentAnalyticsDto contentAnalytics(@Argument String filter, @Argument String contentType) {
        log.info("GQL contentAnalytics(filter={}, contentType={})", filter, contentType);
        return adminService.getContentAnalytics(filter != null ? filter : "month", contentType);
    }

    // =========================================================================
    // PERFORMANCE
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PerformanceMetrics performanceMetrics() {
        log.info("GQL performanceMetrics");
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return PerformanceMetrics.builder()
                .avgResponseTimeMs(0.0)
                .cacheHitRate(0.95)
                .activeConnections(0)
                .uptimeSeconds((System.currentTimeMillis() - getStartTime()) / 1000)
                .build();
    }

    // =========================================================================
    // SELLER ANALYTICS
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public Long sellerProductSales(@ContextValue UUID sellerId) {
        log.info("GQL sellerProductSales(seller={})", sellerId);
        return analyticsService.getProductSales(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public BigDecimal sellerRevenue(@ContextValue UUID sellerId) {
        log.info("GQL sellerRevenue(seller={})", sellerId);
        return analyticsService.getSellerRevenue(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public BigDecimal sellerRevenueForPeriod(
            @Argument LocalDateTime start,
            @Argument LocalDateTime end,
            @ContextValue UUID sellerId) {
        log.info("GQL sellerRevenueForPeriod(seller={}, {}, {})", sellerId, start, end);
        return analyticsService.getSellerRevenueForPeriod(sellerId, start, end);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public Double sellerCancellationRate(@ContextValue UUID sellerId) {
        log.info("GQL sellerCancellationRate(seller={})", sellerId);
        return analyticsService.getSellerCancellationRate(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public Long sellerLowStockCount(@ContextValue UUID sellerId) {
        log.info("GQL sellerLowStockCount(seller={})", sellerId);
        return analyticsService.getLowStockCount(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public AnalyticsService.SellerMetrics sellerMetrics(@ContextValue UUID sellerId) {
        log.info("GQL sellerMetrics(seller={})", sellerId);
        return analyticsService.getSellerMetrics(sellerId);
    }

    // =========================================================================
    // CUSTOMER ANALYTICS
    // =========================================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public BigDecimal customerTotalSpending(@ContextValue UUID userId) {
        log.info("GQL customerTotalSpending(user={})", userId);
        return analyticsService.getCustomerTotalSpending(userId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AnalyticsService.CategoryPreference> customerCategoryPreferences(@ContextValue UUID userId) {
        log.info("GQL customerCategoryPreferences(user={})", userId);
        return analyticsService.getCustomerCategoryPreferences(userId);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static final long START_TIME = System.currentTimeMillis();

    private long getStartTime() {
        return START_TIME;
    }
}

package ecommerce.modules.analytics.service.impl;

import ecommerce.modules.analytics.dto.SellerAnalyticsDto;
import ecommerce.modules.analytics.dto.SellerAnalyticsResponse;
import ecommerce.modules.analytics.dto.SellerDashboardResponse;
import ecommerce.modules.analytics.service.SellerAnalyticsService;
import ecommerce.modules.order.dto.SellerOrderDto;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerAnalyticsServiceImpl implements SellerAnalyticsService {

    private final OrderService orderService;
    private final ProductRepository productRepository;

    @Override
    public SellerDashboardResponse getDashboard(UUID sellerId) {
        log.info("Getting seller dashboard for: {}", sellerId);

        var products = productRepository.findBySeller_PublicId(sellerId, Pageable.unpaged()).getContent();

        long totalProducts = products.size();
        long activeProducts = products.stream().filter(p -> p.getIsActive()).count();

        double averageRating = products.stream()
                .filter(p -> p.getRating() != null)
                .mapToDouble(p -> p.getRating().doubleValue())
                .average()
                .orElse(0.0);

        long storeVisits = products.stream()
                .mapToLong(p -> p.getViewCount() != null ? p.getViewCount().longValue() : 0L)
                .sum();

        long lastMonthVisits = storeVisits > 0 ? (long) (storeVisits * 0.9) : 0;
        double visitGrowth = lastMonthVisits > 0
                ? (double) (storeVisits - lastMonthVisits) / lastMonthVisits * 100 : 0;

        SellerOrderDto orderDashboard = orderService.getSellerOrderDashboard(sellerId);

        return SellerDashboardResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalOrders(orderDashboard.getTotalOrders())
                .ordersThisMonth(orderDashboard.getOrdersThisMonth())
                .pendingOrders(orderDashboard.getPendingOrders())
                .completedOrders(orderDashboard.getCompletedOrders())
                .totalRevenue(orderDashboard.getTotalRevenue())
                .monthlyRevenue(orderDashboard.getMonthlyRevenue())
                .revenueGrowth(orderDashboard.getRevenueGrowth())
                .averageRating(averageRating)
                .totalCustomers(orderDashboard.getTotalCustomers())
                .storeVisits(storeVisits)
                .visitGrowth(visitGrowth)
                .recentOrders(convertRecentOrders(orderDashboard.getRecentOrders()))
                .topProducts(Collections.emptyList())
                .build();
    }

    @Override
    public SellerAnalyticsDto getSellerAnalytics(UUID sellerId) {
        return orderService.getSellerAnalytics(sellerId);
    }

    @Override
    public SellerAnalyticsResponse getSalesAnalytics(UUID sellerId, int days) {
        SellerOrderDto.SellerOrderAnalytics analytics = orderService.getSellerOrderAnalytics(sellerId, days);

        long totalViews = productRepository.findBySeller_PublicId(sellerId, Pageable.unpaged()).getContent().stream()
                .mapToLong(p -> p.getViewCount() != null ? p.getViewCount().longValue() : 0L)
                .sum();
        double conversionRate = totalViews > 0
                ? (double) analytics.getTotalOrders() / totalViews * 100 : 0.0;

        return SellerAnalyticsResponse.builder()
                .totalSales(analytics.getTotalSales())
                .averageOrderValue(analytics.getAverageOrderValue())
                .totalOrders(analytics.getTotalOrders())
                .totalProductsSold(analytics.getTotalProductsSold())
                .conversionRate(conversionRate)
                .dailySales(analytics.getDailySales().stream()
                        .map(s -> SellerAnalyticsResponse.DailySales.builder()
                                .date(s.getDate()).sales(s.getSales()).orders(s.getOrders()).build())
                        .toList())
                .topProducts(analytics.getTopProducts().stream()
                        .map(p -> SellerAnalyticsResponse.TopProduct.builder()
                                .productId(p.getProductId()).productName(p.getProductName())
                                .quantitySold(p.getQuantitySold()).revenue(p.getRevenue()).build())
                        .toList())
                .build();
    }

    private List<SellerDashboardResponse.RecentOrderDto> convertRecentOrders(
            List<SellerOrderDto.RecentSellerOrderDto> recentOrders) {
        if (recentOrders == null) return Collections.emptyList();
        return recentOrders.stream()
                .map(o -> SellerDashboardResponse.RecentOrderDto.builder()
                        .orderId(o.getOrderId()).orderNumber(o.getOrderNumber())
                        .customerName(o.getCustomerName()).productName(o.getProductName())
                        .amount(o.getAmount()).status(o.getStatus()).timeAgo(o.getTimeAgo())
                        .build())
                .toList();
    }
}

package ecommerce.modules.analytics.service.impl;

import ecommerce.modules.analytics.dto.SellerAnalyticsDto;
import ecommerce.modules.analytics.dto.SellerAnalyticsResponse;
import ecommerce.modules.analytics.dto.SellerDashboardResponse;
import ecommerce.modules.analytics.service.SellerAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerAnalyticsServiceImpl implements SellerAnalyticsService {

    @Override
    public SellerDashboardResponse getDashboard(UUID sellerId) {
        log.info("Getting seller dashboard for: {}", sellerId);
        return SellerDashboardResponse.builder()
                .totalProducts(0L)
                .activeProducts(0L)
                .totalOrders(0L)
                .ordersThisMonth(0L)
                .pendingOrders(0L)
                .completedOrders(0L)
                .totalRevenue(BigDecimal.ZERO)
                .monthlyRevenue(BigDecimal.ZERO)
                .revenueGrowth(BigDecimal.ZERO)
                .averageRating(0.0)
                .totalCustomers(0L)
                .storeVisits(0L)
                .visitGrowth(0.0)
                .recentOrders(Collections.emptyList())
                .topProducts(Collections.emptyList())
                .build();
    }

    @Override
    public SellerAnalyticsDto getSellerAnalytics(UUID sellerId) {
        return SellerAnalyticsDto.builder().build();
    }

    @Override
    public SellerAnalyticsResponse getSalesAnalytics(UUID sellerId, int days) {
        return SellerAnalyticsResponse.builder()
                .totalSales(BigDecimal.ZERO)
                .averageOrderValue(BigDecimal.ZERO)
                .totalOrders(0L)
                .totalProductsSold(0L)
                .conversionRate(0.0)
                .dailySales(Collections.emptyList())
                .topProducts(Collections.emptyList())
                .build();
    }
}

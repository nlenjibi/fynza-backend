package ecommerce.modules.analytics.service;

import ecommerce.modules.analytics.dto.SellerAnalyticsDto;
import ecommerce.modules.analytics.dto.SellerAnalyticsResponse;
import ecommerce.modules.analytics.dto.SellerDashboardResponse;

import java.util.UUID;

public interface SellerAnalyticsService {

    SellerDashboardResponse getDashboard(UUID sellerId);

    SellerAnalyticsDto getSellerAnalytics(UUID sellerId);

    SellerAnalyticsResponse getSalesAnalytics(UUID sellerId, int days);
}

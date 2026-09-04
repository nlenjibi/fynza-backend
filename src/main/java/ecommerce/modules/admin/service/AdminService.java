package ecommerce.modules.admin.service;

import ecommerce.modules.admin.dto.AdminAnalyticsDto;
import ecommerce.modules.admin.dto.AdminDashboardDto;
import ecommerce.modules.admin.dto.ContentAnalyticsDto;

public interface AdminService {
    AdminDashboardDto getDashboardStats();

    AdminAnalyticsDto getAnalytics(String filterPeriod);

    ContentAnalyticsDto getContentAnalytics(String filterPeriod, String contentType);
}

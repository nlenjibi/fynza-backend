package ecommerce.modules.analytics.service;

import ecommerce.modules.analytics.dto.AdminAnalyticsDto;
import ecommerce.modules.analytics.dto.AdminDashboardDto;
import ecommerce.modules.analytics.dto.ContentAnalyticsDto;

public interface AdminService {
    AdminDashboardDto getDashboardStats();
    AdminAnalyticsDto getAnalytics(String filter);
    ContentAnalyticsDto getContentAnalytics(String filter, String contentType);
}

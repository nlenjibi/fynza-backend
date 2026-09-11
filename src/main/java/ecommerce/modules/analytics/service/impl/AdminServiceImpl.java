package ecommerce.modules.analytics.service.impl;

import ecommerce.modules.analytics.dto.AdminAnalyticsDto;
import ecommerce.modules.analytics.dto.AdminDashboardDto;
import ecommerce.modules.analytics.dto.ContentAnalyticsDto;
import ecommerce.modules.analytics.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    @Override
    public AdminDashboardDto getDashboardStats() {
        log.debug("AdminServiceImpl.getDashboardStats()");
        return AdminDashboardDto.builder().build();
    }

    @Override
    public AdminAnalyticsDto getAnalytics(String filter) {
        log.debug("AdminServiceImpl.getAnalytics(filter={})", filter);
        return AdminAnalyticsDto.builder()
                .filterPeriod(filter)
                .build();
    }

    @Override
    public ContentAnalyticsDto getContentAnalytics(String filter, String contentType) {
        log.debug("AdminServiceImpl.getContentAnalytics(filter={}, contentType={})", filter, contentType);
        return ContentAnalyticsDto.builder()
                .filterPeriod(filter)
                .build();
    }
}

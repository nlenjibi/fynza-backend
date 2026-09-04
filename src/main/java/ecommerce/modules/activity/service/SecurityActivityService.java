package ecommerce.modules.activity.service;

import ecommerce.modules.activity.entity.SecurityActivity;
import ecommerce.modules.activity.repository.SecurityActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityActivityService {

    private final SecurityActivityRepository securityActivityRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(UUID userId, SecurityActivity.SecurityActivityType type, 
                            String description, String ipAddress, String userAgent,
                            String deviceInfo, String location, String status) {
        SecurityActivity activity = SecurityActivity.builder()
                .userId(userId)
                .activityType(type)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceInfo(deviceInfo)
                .location(location)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        securityActivityRepository.save(activity);
        log.debug("Security activity logged: {} for user: {}", type, userId);
    }

    @Transactional(readOnly = true)
    public Page<SecurityActivity> getUserSecurityActivities(UUID userId, Pageable pageable) {
        return securityActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SecurityActivity> getActivitiesByType(UUID userId, SecurityActivity.SecurityActivityType type, Pageable pageable) {
        return securityActivityRepository.findByUserIdAndActivityTypeOrderByCreatedAtDesc(userId, type, pageable);
    }

    @Transactional(readOnly = true)
    public List<SecurityActivity> getRecentActivities(UUID userId, int days, int limit) {
        return securityActivityRepository.findRecentActivities(
                userId, LocalDateTime.now().minusDays(days), Pageable.ofSize(limit));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getActivitySummary(UUID userId, int days) {
        return securityActivityRepository.getActivitySummary(userId, LocalDateTime.now().minusDays(days));
    }

    @Transactional(readOnly = true)
    public long getFailedLoginCount(UUID userId, int days) {
        return securityActivityRepository.countFailedLogins(userId, LocalDateTime.now().minusDays(days));
    }
}

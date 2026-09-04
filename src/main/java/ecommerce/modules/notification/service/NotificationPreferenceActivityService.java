package ecommerce.modules.notification.service;

import ecommerce.modules.notification.entity.NotificationPreferenceActivity;
import ecommerce.modules.notification.repository.NotificationPreferenceActivityRepository;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceActivityService {

    private final NotificationPreferenceActivityRepository preferenceActivityRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPreferenceChange(UUID userId, String settingName, 
                                   String oldValue, String newValue, 
                                   String channel, String ipAddress) {
        NotificationPreferenceActivity activity = NotificationPreferenceActivity.builder()
                .userId(userId)
                .settingName(settingName)
                .oldValue(oldValue)
                .newValue(newValue)
                .channel(channel)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        preferenceActivityRepository.save(activity);
        log.debug("Notification preference change logged for user: {}, setting: {}", userId, settingName);
    }

    @Transactional(readOnly = true)
    public Page<NotificationPreferenceActivity> getUserPreferenceHistory(UUID userId, Pageable pageable) {
        return preferenceActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<NotificationPreferenceActivity> getSettingHistory(UUID userId, String settingName, Pageable pageable) {
        return preferenceActivityRepository.findByUserIdAndSettingNameOrderByCreatedAtDesc(
                userId, settingName, pageable);
    }

    @Transactional
    public void savePreferences(UUID userId, Map<String, Boolean> preferences, String ipAddress) {
        for (Map.Entry<String, Boolean> entry : preferences.entrySet()) {
            logPreferenceChange(userId, entry.getKey(), null, 
                entry.getValue() ? "enabled" : "disabled", null, ipAddress);
        }
    }
}

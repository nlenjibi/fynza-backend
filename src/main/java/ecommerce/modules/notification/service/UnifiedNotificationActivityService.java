package ecommerce.modules.notification.service;

import ecommerce.modules.notification.entity.UnifiedNotificationActivity;
import ecommerce.modules.notification.entity.UnifiedNotificationActivity.*;
import ecommerce.modules.notification.repository.UnifiedNotificationActivityRepository;
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
public class UnifiedNotificationActivityService {

    private final UnifiedNotificationActivityRepository unifiedRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(UUID userId, ActivityType type, Category category,
                           NotificationType notificationType, String title, String message,
                           UUID relatedId, String senderType, UUID senderId) {
        UnifiedNotificationActivity activity = UnifiedNotificationActivity.builder()
                .userId(userId)
                .activityType(type)
                .category(category)
                .notificationType(notificationType)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .senderType(senderType)
                .senderId(senderId)
                .createdAt(LocalDateTime.now())
                .build();
        unifiedRepository.save(activity);
        log.debug("Unified notification activity logged for user: {}, type: {}", userId, type);
    }

    @Transactional(readOnly = true)
    public Page<UnifiedNotificationActivity> getNotifications(UUID userId, Pageable pageable) {
        return unifiedRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(
                userId, Category.NOTIFICATION, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UnifiedNotificationActivity> getMessages(UUID userId, Pageable pageable) {
        return unifiedRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(
                userId, Category.MESSAGE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<UnifiedNotificationActivity> getUnread(UUID userId, Pageable pageable) {
        return unifiedRepository.findUnread(userId, pageable);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        unifiedRepository.findById(notificationId).ifPresent(activity -> {
            activity.setIsRead(true);
            activity.setReadAt(LocalDateTime.now());
            activity.setActivityType(ActivityType.READ);
            unifiedRepository.save(activity);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId, Category category) {
        Page<UnifiedNotificationActivity> unread = unifiedRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                userId, false, Pageable.unpaged());
        unread.getContent().forEach(activity -> {
            activity.setIsRead(true);
            activity.setReadAt(LocalDateTime.now());
        });
        unifiedRepository.saveAll(unread.getContent());
    }

    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        unifiedRepository.findById(notificationId).ifPresent(activity -> {
            activity.setActivityType(ActivityType.DELETED);
            unifiedRepository.save(activity);
        });
    }

    @Transactional
    public void pinNotification(UUID userId, UUID notificationId) {
        unifiedRepository.findById(notificationId).ifPresent(activity -> {
            activity.setIsPinned(true);
            activity.setActivityType(ActivityType.PINNED);
            unifiedRepository.save(activity);
        });
    }

    @Transactional
    public void unpinNotification(UUID userId, UUID notificationId) {
        unifiedRepository.findById(notificationId).ifPresent(activity -> {
            activity.setIsPinned(false);
            activity.setActivityType(ActivityType.UNPINNED);
            unifiedRepository.save(activity);
        });
    }

    @Transactional
    public void archiveNotification(UUID userId, UUID notificationId) {
        unifiedRepository.findById(notificationId).ifPresent(activity -> {
            activity.setIsArchived(true);
            activity.setActivityType(ActivityType.ARCHIVED);
            unifiedRepository.save(activity);
        });
    }

    @Transactional
    public void sendMessage(UUID userId, UUID recipientId, String subject, String message,
                           String senderType, UUID senderId) {
        logActivity(userId, ActivityType.MESSAGE_SENT, Category.MESSAGE, NotificationType.valueOf(senderType.toUpperCase()),
                subject, message, recipientId, senderType, senderId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId, Category category) {
        return unifiedRepository.countUnreadByCategory(userId, category);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTypeSummary(UUID userId, int days) {
        return unifiedRepository.getTypeSummary(userId, LocalDateTime.now().minusDays(days));
    }
}

package ecommerce.graphql.resolver.notification;

import ecommerce.common.cache.CacheNames;
import ecommerce.common.cache.CacheService;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.notification.dto.NotificationPreferenceResponse;
import ecommerce.modules.notification.dto.QuietHoursResponse;
import ecommerce.modules.notification.entity.NotificationPreference;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationType;
import ecommerce.modules.notification.repository.NotificationPreferenceRepository;
import ecommerce.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationMutationResolver {

    private final NotificationService              notificationService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final CacheService                     cacheService;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean markNotificationRead(
            @Argument String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(UUID.fromString(id), principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean markAllNotificationsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteNotification(
            @Argument String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.softDelete(UUID.fromString(id), principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAllNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.softDeleteAll(principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public NotificationPreferenceResponse updateNotificationPreference(
            @Argument String notificationType,
            @Argument String channel,
            @Argument boolean enabled,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationType    type   = NotificationType.valueOf(notificationType);
        NotificationChannel chan   = NotificationChannel.valueOf(channel);
        UUID                userId = principal.getId();

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, type, chan)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId).notificationType(type).channel(chan).build());

        pref.setEnabled(enabled);
        pref = preferenceRepository.save(pref);

        cacheService.evict(CacheNames.NOTIFICATION_PREF + ":" + userId + ":" + type.name() + ":" + chan.name());

        log.info("[Notification] Preference updated userId={} type={} channel={} enabled={}",
                userId, type, chan, enabled);
        return NotificationPreferenceResponse.from(pref);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public QuietHoursResponse updateQuietHours(
            @Argument String startTime,
            @Argument String endTime,
            @Argument String timezone,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.updateQuietHours(principal.getId(), startTime, endTime, timezone);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public boolean deleteQuietHours(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.deleteQuietHours(principal.getId());
        return true;
    }
}

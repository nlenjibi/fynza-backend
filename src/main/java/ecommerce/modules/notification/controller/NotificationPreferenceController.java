package ecommerce.modules.notification.controller;

import ecommerce.common.cache.CacheNames;
import ecommerce.common.cache.CacheService;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.notification.dto.NotificationPreferenceResponse;
import ecommerce.modules.notification.dto.QuietHoursRequest;
import ecommerce.modules.notification.dto.QuietHoursResponse;
import ecommerce.modules.notification.dto.UpdatePreferenceRequest;
import ecommerce.modules.notification.entity.NotificationPreference;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationType;
import ecommerce.modules.notification.repository.NotificationPreferenceRepository;
import ecommerce.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/notification-preferences")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notification Preferences", description = "Manage per-channel notification preferences and quiet hours")
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationService              notificationService;
    private final CacheService                     cacheService;

    // ── Channel Preferences ──────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all notification preferences for the current user")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationPreferenceResponse> prefs = preferenceRepository
                .findByUserId(principal.getId())
                .stream()
                .map(NotificationPreferenceResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Preferences retrieved", prefs));
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Upsert a notification preference for a type + channel combination")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> upsert(
            @Valid @RequestBody UpdatePreferenceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        NotificationType    type = NotificationType.valueOf(request.getNotificationType());
        NotificationChannel chan = NotificationChannel.valueOf(request.getChannel());

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, type, chan)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId).notificationType(type).channel(chan).build());

        pref.setEnabled(request.isEnabled());
        pref = preferenceRepository.save(pref);

        cacheService.evict(CacheNames.NOTIFICATION_PREF + ":" + userId + ":" + type.name() + ":" + chan.name());

        return ResponseEntity.ok(ApiResponse.success("Preference updated", NotificationPreferenceResponse.from(pref)));
    }

    @DeleteMapping
    @Transactional
    @Operation(summary = "Reset all notification preferences for the current user (removes all rows — defaults to opt-in)")
    public ResponseEntity<Void> reset(@AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        preferenceRepository.deleteAll(preferenceRepository.findByUserId(userId));
        cacheService.clear(CacheNames.NOTIFICATION_PREF + ":" + userId + ":*");
        return ResponseEntity.noContent().build();
    }

    // ── Quiet Hours ──────────────────────────────────────────────────────────

    @GetMapping("/quiet-hours")
    @Operation(summary = "Get the current user's quiet hours config (204 if none set)")
    public ResponseEntity<ApiResponse<QuietHoursResponse>> getQuietHours(
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.getQuietHours(principal.getId())
                .map(qh -> ResponseEntity.ok(ApiResponse.success("Quiet hours retrieved", qh)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/quiet-hours")
    @Transactional
    @Operation(summary = "Set or update the current user's quiet hours (push and SMS are suppressed during this window)")
    public ResponseEntity<ApiResponse<QuietHoursResponse>> updateQuietHours(
            @Valid @RequestBody QuietHoursRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        QuietHoursResponse response = notificationService.updateQuietHours(
                principal.getId(), request.getStartTime(), request.getEndTime(), request.getTimezone());
        return ResponseEntity.ok(ApiResponse.success("Quiet hours updated", response));
    }

    @DeleteMapping("/quiet-hours")
    @Transactional
    @Operation(summary = "Remove the current user's quiet hours config")
    public ResponseEntity<Void> deleteQuietHours(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.deleteQuietHours(principal.getId());
        return ResponseEntity.noContent().build();
    }
}

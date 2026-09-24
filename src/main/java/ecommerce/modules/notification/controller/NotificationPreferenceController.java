package ecommerce.modules.notification.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.notification.dto.NotificationPreferenceResponse;
import ecommerce.modules.notification.dto.UpdatePreferenceRequest;
import ecommerce.modules.notification.entity.NotificationPreference;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationType;
import ecommerce.modules.notification.repository.NotificationPreferenceRepository;
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
@Tag(name = "Notification Preferences", description = "Manage per-channel notification opt-in/opt-out preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository preferenceRepository;

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
        NotificationType type    = NotificationType.valueOf(request.getNotificationType());
        NotificationChannel chan = NotificationChannel.valueOf(request.getChannel());

        NotificationPreference pref = preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, type, chan)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId)
                        .notificationType(type)
                        .channel(chan)
                        .build());

        pref.setEnabled(request.isEnabled());
        pref = preferenceRepository.save(pref);
        return ResponseEntity.ok(ApiResponse.success("Preference updated", NotificationPreferenceResponse.from(pref)));
    }

    @DeleteMapping
    @Transactional
    @Operation(summary = "Reset all notification preferences for the current user (removes all rows — defaults to opt-in)")
    public ResponseEntity<Void> reset(@AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationPreference> existing = preferenceRepository.findByUserId(principal.getId());
        preferenceRepository.deleteAll(existing);
        return ResponseEntity.noContent().build();
    }
}

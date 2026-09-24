package ecommerce.graphql.resolver.notification;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.notification.dto.NotificationPreferenceResponse;
import ecommerce.modules.notification.dto.NotificationResponse;
import ecommerce.modules.notification.dto.QuietHoursResponse;
import ecommerce.modules.notification.repository.NotificationPreferenceRepository;
import ecommerce.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationQueryResolver {

    private final NotificationService              notificationService;
    private final NotificationPreferenceRepository preferenceRepository;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> notifications(
            @Argument int page,
            @Argument int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<NotificationResponse> result =
                notificationService.getForRecipient(principal.getId(), PageRequest.of(page, Math.min(size, 50)));
        return Map.of(
                "content",       result.getContent(),
                "totalElements", (int) result.getTotalElements(),
                "totalPages",    result.getTotalPages(),
                "currentPage",   result.getNumber(),
                "hasNextPage",   result.hasNext()
        );
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public NotificationResponse notification(
            @Argument String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.getById(java.util.UUID.fromString(id), principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public int unreadNotificationCount(@AuthenticationPrincipal UserPrincipal principal) {
        return (int) notificationService.countUnread(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<NotificationPreferenceResponse> notificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return preferenceRepository.findByUserId(principal.getId())
                .stream()
                .map(NotificationPreferenceResponse::from)
                .collect(Collectors.toList());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public QuietHoursResponse quietHours(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.getQuietHours(principal.getId()).orElse(null);
    }
}

package ecommerce.modules.notification.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.notification.dto.NotificationDeviceResponse;
import ecommerce.modules.notification.dto.RegisterDeviceRequest;
import ecommerce.modules.notification.entity.NotificationDevice;
import ecommerce.modules.notification.enums.DevicePlatform;
import ecommerce.modules.notification.enums.DeviceStatus;
import ecommerce.modules.notification.repository.NotificationDeviceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/notification-devices")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notification Devices", description = "Register and remove push notification device tokens")
public class NotificationDeviceController {

    private final NotificationDeviceRepository deviceRepository;

    @PostMapping
    @Transactional
    @Operation(summary = "Register a device token for push notifications")
    public ResponseEntity<ApiResponse<NotificationDeviceResponse>> register(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationDevice device = deviceRepository
                .findByDeviceToken(request.getDeviceToken())
                .map(existing -> {
                    // Re-activate if previously deactivated, refresh last seen
                    existing.setStatus(DeviceStatus.ACTIVE);
                    existing.setUserId(principal.getId());
                    existing.setAppVersion(request.getAppVersion());
                    existing.setLastSeenAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> NotificationDevice.builder()
                        .userId(principal.getId())
                        .platform(DevicePlatform.valueOf(request.getPlatform()))
                        .deviceToken(request.getDeviceToken())
                        .appVersion(request.getAppVersion())
                        .status(DeviceStatus.ACTIVE)
                        .build());

        device = deviceRepository.save(device);
        log.info("[Device] Registered userId={} platform={} publicId={}",
                principal.getId(), device.getPlatform(), device.getPublicId());
        return ResponseEntity.ok(ApiResponse.success("Device registered", NotificationDeviceResponse.from(device)));
    }

    @DeleteMapping("/{deviceId}")
    @Transactional
    @Operation(summary = "Deactivate a push notification device token")
    public ResponseEntity<Void> remove(
            @PathVariable UUID deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        deviceRepository.findByPublicIdAndUserId(deviceId, principal.getId())
                .ifPresent(d -> {
                    d.setStatus(DeviceStatus.INACTIVE);
                    deviceRepository.save(d);
                    log.info("[Device] Deactivated publicId={} userId={}", deviceId, principal.getId());
                });
        return ResponseEntity.noContent().build();
    }
}

package ecommerce.modules.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendSecurityNotification(UUID userId, String type, String message) {
        log.info("Sending security notification to user: {}, type: {}", userId, type);
        
        Map<String, Object> notification = Map.of(
            "type", type,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSendToUser(
            userId.toString(), 
            "/queue/security", 
            notification
        );
        
        log.debug("Security notification sent successfully to user: {}", userId);
    }

    public void sendPasswordChangedNotification(UUID userId, String ipAddress) {
        sendSecurityNotification(userId, "PASSWORD_CHANGED", "Password changed from " + ipAddress);
    }

    public void send2FAEnabledNotification(UUID userId) {
        sendSecurityNotification(userId, "2FA_ENABLED", "Two-factor authentication was enabled");
    }

    public void send2FADisabledNotification(UUID userId) {
        sendSecurityNotification(userId, "2FA_DISABLED", "Two-factor authentication was disabled");
    }

    public void sendSuspiciousActivityAlert(UUID userId, String ipAddress, String location) {
        sendSecurityNotification(userId, "SUSPICIOUS_ACTIVITY", "Suspicious login from " + ipAddress + " (" + location + ")");
    }

    public void sendAccountLockedNotification(UUID userId, String ipAddress) {
        sendSecurityNotification(userId, "ACCOUNT_LOCKED", "Account locked due to multiple failed login attempts from " + ipAddress);
    }

    public void sendAccountUnlockedNotification(UUID userId) {
        sendSecurityNotification(userId, "ACCOUNT_UNLOCKED", "Your account has been unlocked");
    }
}

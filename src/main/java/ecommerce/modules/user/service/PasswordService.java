package ecommerce.modules.user.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.activity.entity.SecurityActivity;
import ecommerce.modules.activity.service.SecurityActivityService;
import ecommerce.modules.user.entity.PasswordHistory;
import ecommerce.modules.user.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final SecurityActivityService securityActivityService;
    private final PasswordEncoder passwordEncoder;

    private static final int MIN_PASSWORD_HISTORY = 5;

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword,
                              String ipAddress, String userAgent) {
        log.info("Changing password for user: {}", userId);

        PasswordHistory current = passwordHistoryRepository.findCurrentPassword(userId);
        
        if (current == null || !passwordEncoder.matches(currentPassword, current.getPasswordHash())) {
            securityActivityService.logActivity(userId, 
                SecurityActivity.SecurityActivityType.PASSWORD_CHANGE_FAILED,
                "Failed password change attempt", ipAddress, userAgent, null, null, "FAILED");
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, current.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        checkPasswordHistory(userId, newPassword);

        current.setIsCurrent(false);
        passwordHistoryRepository.save(current);

        PasswordHistory newPasswordHistory = PasswordHistory.builder()
                .userId(userId)
                .passwordHash(passwordEncoder.encode(newPassword))
                .changedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .isCurrent(true)
                .build();
        passwordHistoryRepository.save(newPasswordHistory);

        securityActivityService.logActivity(userId,
            SecurityActivity.SecurityActivityType.PASSWORD_CHANGED,
            "Password changed successfully", ipAddress, userAgent, null, null, "SUCCESS");

        log.info("Password changed successfully for user: {}", userId);
    }

    private void checkPasswordHistory(UUID userId, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByChangedAtDesc(userId);
        
        int count = 0;
        for (PasswordHistory old : history) {
            if (count >= MIN_PASSWORD_HISTORY) {
                break;
            }
            if (passwordEncoder.matches(newPassword, old.getPasswordHash())) {
                throw new BadRequestException("Cannot reuse any of your last " + MIN_PASSWORD_HISTORY + " passwords");
            }
            count++;
        }
    }
}

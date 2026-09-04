package ecommerce.modules.user.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.activity.entity.SecurityActivity;
import ecommerce.modules.activity.service.SecurityActivityService;
import ecommerce.modules.user.entity.TwoFactorAuth;
import ecommerce.modules.user.repository.TwoFactorAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final SecurityActivityService securityActivityService;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Transactional
    public String initiateSetup(UUID userId, String ipAddress, String userAgent) {
        log.info("Initiating 2FA setup for user: {}", userId);

        if (twoFactorAuthRepository.findByUserId(userId).isPresent()) {
            throw new BadRequestException("2FA is already set up");
        }

        String secret = generateSecret();
        TwoFactorAuth tfa = TwoFactorAuth.builder()
                .userId(userId)
                .secretKey(secret)
                .isEnabled(false)
                .build();
        twoFactorAuthRepository.save(tfa);

        securityActivityService.logActivity(userId,
            SecurityActivity.SecurityActivityType.TWO_FA_SETUP_INITIATED,
            "2FA setup initiated", ipAddress, userAgent, null, null, "INITIATED");

        return secret;
    }

    @Transactional
    public void completeSetup(UUID userId, String verificationCode, String ipAddress, String userAgent) {
        log.info("Completing 2FA setup for user: {}", userId);

        TwoFactorAuth tfa = twoFactorAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("2FA setup not initiated"));

        if (!verifyCodeInternal(tfa.getSecretKey(), verificationCode)) {
            securityActivityService.logActivity(userId,
                SecurityActivity.SecurityActivityType.TWO_FA_SETUP_FAILED,
                "2FA setup verification failed", ipAddress, userAgent, null, null, "FAILED");
            throw new BadRequestException("Invalid verification code");
        }

        String[] recoveryCodes = generateRecoveryCodes();
        tfa.setIsEnabled(true);
        tfa.setEnabledAt(LocalDateTime.now());
        tfa.setRecoveryCodes(String.join(",", recoveryCodes));
        tfa.setFailedAttempts(0);
        twoFactorAuthRepository.save(tfa);

        securityActivityService.logActivity(userId,
            SecurityActivity.SecurityActivityType.TWO_FA_SETUP_COMPLETED,
            "2FA enabled successfully", ipAddress, userAgent, null, null, "SUCCESS");

        log.info("2FA enabled successfully for user: {}", userId);
    }

    @Transactional
    public boolean verifyCode(UUID userId, String code, String ipAddress, String userAgent) {
        log.debug("Verifying 2FA code for user: {}", userId);

        TwoFactorAuth tfa = twoFactorAuthRepository.findEnabledByUserId(userId)
                .orElseThrow(() -> new BadRequestException("2FA not enabled"));

        if (tfa.getLockedUntil() != null && tfa.getLockedUntil().isAfter(LocalDateTime.now())) {
            securityActivityService.logActivity(userId,
                SecurityActivity.SecurityActivityType.TWO_FA_VERIFICATION_FAILED,
                "2FA verification failed - account locked", ipAddress, userAgent, null, null, "LOCKED");
            return false;
        }

        if (Arrays.asList(tfa.getRecoveryCodes().split(",")).contains(code)) {
            tfa.setBackupCodesUsed(tfa.getBackupCodesUsed() + 1);
            tfa.setFailedAttempts(0);
            tfa.setLastVerifiedAt(LocalDateTime.now());
            twoFactorAuthRepository.save(tfa);

            securityActivityService.logActivity(userId,
                SecurityActivity.SecurityActivityType.TWO_FA_BACKUP_CODES_USED,
                "2FA verified via backup code", ipAddress, userAgent, null, null, "SUCCESS");
            return true;
        }

        if (!verifyCodeInternal(tfa.getSecretKey(), code)) {
            int attempts = tfa.getFailedAttempts() + 1;
            tfa.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                tfa.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            twoFactorAuthRepository.save(tfa);

            securityActivityService.logActivity(userId,
                SecurityActivity.SecurityActivityType.TWO_FA_VERIFICATION_FAILED,
                "2FA verification failed", ipAddress, userAgent, null, null, "FAILED");
            return false;
        }

        tfa.setFailedAttempts(0);
        tfa.setLastVerifiedAt(LocalDateTime.now());
        twoFactorAuthRepository.save(tfa);

        securityActivityService.logActivity(userId,
            SecurityActivity.SecurityActivityType.TWO_FA_VERIFICATION_SUCCESS,
            "2FA verified successfully", ipAddress, userAgent, null, null, "SUCCESS");

        return true;
    }

    @Transactional
    public void disable(UUID userId, String ipAddress, String userAgent) {
        log.info("Disabling 2FA for user: {}", userId);

        TwoFactorAuth tfa = twoFactorAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("2FA not enabled"));

        twoFactorAuthRepository.delete(tfa);

        securityActivityService.logActivity(userId,
            SecurityActivity.SecurityActivityType.TWO_FA_DISABLED,
            "2FA disabled", ipAddress, userAgent, null, null, "SUCCESS");

        log.info("2FA disabled for user: {}", userId);
    }

    public Optional<TwoFactorAuth> getTwoFactorAuth(UUID userId) {
        return twoFactorAuthRepository.findByUserId(userId);
    }

    public boolean is2FAEnabled(UUID userId) {
        return twoFactorAuthRepository.findEnabledByUserId(userId).isPresent();
    }

    private boolean verifyCodeInternal(String secret, String code) {
        return code != null && code.length() == 6 && code.matches("\\d+");
    }

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private String[] generateRecoveryCodes() {
        String[] codes = new String[8];
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < codes.length; i++) {
            codes[i] = String.format("%04X-%04X", random.nextInt(65536), random.nextInt(65536));
        }
        return codes;
    }
}

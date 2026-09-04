package ecommerce.modules.notification.service;

import ecommerce.modules.notification.entity.NotificationSettings;
import ecommerce.modules.notification.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationPreferenceActivityService preferenceActivityService;

    @Transactional(readOnly = true)
    public NotificationSettings getSettings(UUID userId) {
        return notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    @Transactional
    public NotificationSettings updateSettings(UUID userId, NotificationSettings newSettings, String ipAddress) {
        NotificationSettings current = getSettings(userId);

        if (!current.getIsOrderUpdates().equals(newSettings.getIsOrderUpdates())) {
            preferenceActivityService.logPreferenceChange(userId, "ORDER_UPDATES",
                current.getIsOrderUpdates() ? "enabled" : "disabled",
                newSettings.getIsOrderUpdates() ? "enabled" : "disabled", "IN_APP", ipAddress);
        }

        if (!current.getIsPaymentConfirmation().equals(newSettings.getIsPaymentConfirmation())) {
            preferenceActivityService.logPreferenceChange(userId, "PAYMENT_CONFIRMATION",
                current.getIsPaymentConfirmation() ? "enabled" : "disabled",
                newSettings.getIsPaymentConfirmation() ? "enabled" : "disabled", "IN_APP", ipAddress);
        }

        if (!current.getIsShippingUpdates().equals(newSettings.getIsShippingUpdates())) {
            preferenceActivityService.logPreferenceChange(userId, "SHIPPING_UPDATES",
                current.getIsShippingUpdates() ? "enabled" : "disabled",
                newSettings.getIsShippingUpdates() ? "enabled" : "disabled", "IN_APP", ipAddress);
        }

        if (!current.getIsPromotionalEmail().equals(newSettings.getIsPromotionalEmail())) {
            preferenceActivityService.logPreferenceChange(userId, "PROMOTIONAL_EMAIL",
                current.getIsPromotionalEmail() ? "enabled" : "disabled",
                newSettings.getIsPromotionalEmail() ? "enabled" : "disabled", "EMAIL", ipAddress);
        }

        if (!current.getIsPromotionalSms().equals(newSettings.getIsPromotionalSms())) {
            preferenceActivityService.logPreferenceChange(userId, "PROMOTIONAL_SMS",
                current.getIsPromotionalSms() ? "enabled" : "disabled",
                newSettings.getIsPromotionalSms() ? "enabled" : "disabled", "SMS", ipAddress);
        }

        if (!current.getIsNewProductAlerts().equals(newSettings.getIsNewProductAlerts())) {
            preferenceActivityService.logPreferenceChange(userId, "NEW_PRODUCT_ALERTS",
                current.getIsNewProductAlerts() ? "enabled" : "disabled",
                newSettings.getIsNewProductAlerts() ? "enabled" : "disabled", "EMAIL", ipAddress);
        }

        if (!current.getIsPriceDropAlerts().equals(newSettings.getIsPriceDropAlerts())) {
            preferenceActivityService.logPreferenceChange(userId, "PRICE_DROP_ALERTS",
                current.getIsPriceDropAlerts() ? "enabled" : "disabled",
                newSettings.getIsPriceDropAlerts() ? "enabled" : "disabled", "EMAIL", ipAddress);
        }

        if (!current.getIsWishlistUpdates().equals(newSettings.getIsWishlistUpdates())) {
            preferenceActivityService.logPreferenceChange(userId, "WISHLIST_UPDATES",
                current.getIsWishlistUpdates() ? "enabled" : "disabled",
                newSettings.getIsWishlistUpdates() ? "enabled" : "disabled", "EMAIL", ipAddress);
        }

        if (!current.getIsReviewRequests().equals(newSettings.getIsReviewRequests())) {
            preferenceActivityService.logPreferenceChange(userId, "REVIEW_REQUESTS",
                current.getIsReviewRequests() ? "enabled" : "disabled",
                newSettings.getIsReviewRequests() ? "enabled" : "disabled", "EMAIL", ipAddress);
        }

        if (!current.getIsNewsletter().equals(newSettings.getIsNewsletter())) {
            preferenceActivityService.logPreferenceChange(userId, "NEWSLETTER",
                current.getIsNewsletter() ? "enabled" : "disabled",
                newSettings.getIsNewsletter() ? "enabled" : "disabled", "EMAIL", ipAddress);
        }

        if (!current.getIsBrowserPush().equals(newSettings.getIsBrowserPush())) {
            preferenceActivityService.logPreferenceChange(userId, "BROWSER_PUSH",
                current.getIsBrowserPush() ? "enabled" : "disabled",
                newSettings.getIsBrowserPush() ? "enabled" : "disabled", "PUSH", ipAddress);
        }

        if (!current.getIsAppPush().equals(newSettings.getIsAppPush())) {
            preferenceActivityService.logPreferenceChange(userId, "APP_PUSH",
                current.getIsAppPush() ? "enabled" : "disabled",
                newSettings.getIsAppPush() ? "enabled" : "disabled", "PUSH", ipAddress);
        }

        newSettings.setId(current.getId());
        newSettings.setUserId(userId);
        return notificationSettingsRepository.save(newSettings);
    }

    private NotificationSettings createDefaultSettings(UUID userId) {
        NotificationSettings settings = NotificationSettings.builder()
                .userId(userId)
                .isOrderUpdates(true)
                .isPaymentConfirmation(true)
                .isShippingUpdates(true)
                .isPromotionalEmail(true)
                .isPromotionalSms(false)
                .isNewProductAlerts(true)
                .isPriceDropAlerts(true)
                .isWishlistUpdates(true)
                .isReviewRequests(true)
                .isNewsletter(false)
                .isBrowserPush(true)
                .isAppPush(true)
                .build();
        return notificationSettingsRepository.save(settings);
    }
}

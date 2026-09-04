package ecommerce.modules.settings.service;

import ecommerce.modules.settings.dto.*;
import ecommerce.modules.settings.entity.SiteSettings;
import ecommerce.modules.settings.entity.SocialLinks;
import ecommerce.modules.settings.repository.SiteSettingsRepository;
import ecommerce.modules.settings.repository.SocialLinksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSettingsService {

    private final SiteSettingsRepository siteSettingsRepository;
    private final SocialLinksRepository socialLinksRepository;

    private SiteSettings getOrCreateSettings() {
        return siteSettingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    log.info("Creating default site settings");
                    return siteSettingsRepository.save(SiteSettings.builder().build());
                });
    }

    @Cacheable(value = "settings", key = "'general'")
    @Transactional(readOnly = true)
    public GeneralSettingsResponse getGeneralSettings() {
        log.debug("Fetching general settings");
        SiteSettings settings = getOrCreateSettings();
        return GeneralSettingsResponse.builder()
                .id(settings.getId())
                .siteName(settings.getSiteName())
                .siteEmail(settings.getSiteEmail())
                .sitePhone(settings.getSitePhone())
                .currency(settings.getCurrency())
                .timezone(settings.getTimezone())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public GeneralSettingsResponse updateGeneralSettings(GeneralSettingsRequest request) {
        log.info("Updating general settings");
        SiteSettings settings = getOrCreateSettings();
        settings.setSiteName(request.getSiteName());
        settings.setSiteEmail(request.getSiteEmail());
        settings.setSitePhone(request.getSitePhone());
        settings.setCurrency(request.getCurrency());
        settings.setTimezone(request.getTimezone());
        settings = siteSettingsRepository.save(settings);
        return GeneralSettingsResponse.builder()
                .id(settings.getId())
                .siteName(settings.getSiteName())
                .siteEmail(settings.getSiteEmail())
                .sitePhone(settings.getSitePhone())
                .currency(settings.getCurrency())
                .timezone(settings.getTimezone())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @Cacheable(value = "settings", key = "'payment'")
    @Transactional(readOnly = true)
    public PaymentSettingsResponse getPaymentSettings() {
        log.debug("Fetching payment settings");
        SiteSettings settings = getOrCreateSettings();
        return PaymentSettingsResponse.builder()
                .id(settings.getId())
                .paystackPublicKey(maskKey(settings.getPaystackPublicKey()))
                .enableCashOnDelivery(settings.getEnableCashOnDelivery())
                .enableMobileMoney(settings.getEnableMobileMoney())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public PaymentSettingsResponse updatePaymentSettings(PaymentSettingsRequest request) {
        log.info("Updating payment settings");
        SiteSettings settings = getOrCreateSettings();
        settings.setPaystackPublicKey(request.getPaystackPublicKey());
        settings.setPaystackSecretKey(request.getPaystackSecretKey());
        settings.setEnableCashOnDelivery(request.getEnableCashOnDelivery());
        settings.setEnableMobileMoney(request.getEnableMobileMoney());
        settings = siteSettingsRepository.save(settings);
        return PaymentSettingsResponse.builder()
                .id(settings.getId())
                .paystackPublicKey(maskKey(settings.getPaystackPublicKey()))
                .enableCashOnDelivery(settings.getEnableCashOnDelivery())
                .enableMobileMoney(settings.getEnableMobileMoney())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
    @Cacheable(value = "settings", key = "'shipping'")
    @Transactional(readOnly = true)
    public ShippingSettingsResponse getShippingSettings() {
        SiteSettings settings = getOrCreateSettings();
        return ShippingSettingsResponse.builder()
                .id(settings.getId())
                .shippingCost(settings.getShippingCost())
                .freeShippingThreshold(settings.getFreeShippingThreshold())
                .estimatedDeliveryDays(settings.getEstimatedDeliveryDays())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public ShippingSettingsResponse updateShippingSettings(ShippingSettingsRequest request) {
        SiteSettings settings = getOrCreateSettings();
        settings.setShippingCost(request.getShippingCost());
        settings.setFreeShippingThreshold(request.getFreeShippingThreshold());
        settings.setEstimatedDeliveryDays(request.getEstimatedDeliveryDays());
        settings = siteSettingsRepository.save(settings);
        return ShippingSettingsResponse.builder()
                .id(settings.getId())
                .shippingCost(settings.getShippingCost())
                .freeShippingThreshold(settings.getFreeShippingThreshold())
                .estimatedDeliveryDays(settings.getEstimatedDeliveryDays())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @Cacheable(value = "settings", key = "'tax'")
    @Transactional(readOnly = true)
    public TaxSettingsResponse getTaxSettings() {
        SiteSettings settings = getOrCreateSettings();
        return TaxSettingsResponse.builder()
                .id(settings.getId())
                .taxRate(settings.getTaxRate())
                .taxNumber(settings.getTaxNumber())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public TaxSettingsResponse updateTaxSettings(TaxSettingsRequest request) {
        SiteSettings settings = getOrCreateSettings();
        settings.setTaxRate(request.getTaxRate());
        settings.setTaxNumber(request.getTaxNumber());
        settings = siteSettingsRepository.save(settings);
        return TaxSettingsResponse.builder()
                .id(settings.getId())
                .taxRate(settings.getTaxRate())
                .taxNumber(settings.getTaxNumber())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
    @Cacheable(value = "settings", key = "'email'")
    @Transactional(readOnly = true)
    public EmailSettingsResponse getEmailSettings() {
        SiteSettings settings = getOrCreateSettings();
        return EmailSettingsResponse.builder()
                .id(settings.getId())
                .smtpHost(settings.getSmtpHost())
                .smtpPort(settings.getSmtpPort())
                .smtpEmail(settings.getSmtpEmail())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public EmailSettingsResponse updateEmailSettings(EmailSettingsRequest request) {
        SiteSettings settings = getOrCreateSettings();
        settings.setSmtpHost(request.getSmtpHost());
        settings.setSmtpPort(request.getSmtpPort());
        settings.setSmtpEmail(request.getSmtpEmail());
        settings.setSmtpPassword(request.getSmtpPassword());
        settings = siteSettingsRepository.save(settings);
        return EmailSettingsResponse.builder()
                .id(settings.getId())
                .smtpHost(settings.getSmtpHost())
                .smtpPort(settings.getSmtpPort())
                .smtpEmail(settings.getSmtpEmail())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @Cacheable(value = "settings", key = "'notifications'")
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings() {
        SiteSettings settings = getOrCreateSettings();
        return NotificationSettingsResponse.builder()
                .id(settings.getId())
                .orderUpdates(settings.getEnableOrderUpdates())
                .paymentConfirmation(settings.getEnablePaymentConfirmation())
                .shippingUpdates(settings.getEnableShippingUpdates())
                .promotionalEmails(settings.getEnablePromotionalEmails())
                .newProductAlerts(settings.getEnableNewProductAlerts())
                .priceDropAlerts(settings.getEnablePriceDropAlerts())
                .wishlistUpdates(settings.getEnableWishlistUpdates())
                .reviewRequests(settings.getEnableReviewRequests())
                .newsletter(settings.getEnableNewsletter())
                .promotionalSms(settings.getEnablePromotionalSms())
                .browserNotifications(settings.getEnableBrowserNotifications())
                .appNotifications(settings.getEnableAppNotifications())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(NotificationSettingsRequest request) {
        SiteSettings settings = getOrCreateSettings();
        if (request.getOrderUpdates() != null) settings.setEnableOrderUpdates(request.getOrderUpdates());
        if (request.getPaymentConfirmation() != null) settings.setEnablePaymentConfirmation(request.getPaymentConfirmation());
        if (request.getShippingUpdates() != null) settings.setEnableShippingUpdates(request.getShippingUpdates());
        if (request.getPromotionalEmails() != null) settings.setEnablePromotionalEmails(request.getPromotionalEmails());
        if (request.getNewProductAlerts() != null) settings.setEnableNewProductAlerts(request.getNewProductAlerts());
        if (request.getPriceDropAlerts() != null) settings.setEnablePriceDropAlerts(request.getPriceDropAlerts());
        if (request.getWishlistUpdates() != null) settings.setEnableWishlistUpdates(request.getWishlistUpdates());
        if (request.getReviewRequests() != null) settings.setEnableReviewRequests(request.getReviewRequests());
        if (request.getNewsletter() != null) settings.setEnableNewsletter(request.getNewsletter());
        if (request.getPromotionalSms() != null) settings.setEnablePromotionalSms(request.getPromotionalSms());
        if (request.getBrowserNotifications() != null) settings.setEnableBrowserNotifications(request.getBrowserNotifications());
        if (request.getAppNotifications() != null) settings.setEnableAppNotifications(request.getAppNotifications());
        settings = siteSettingsRepository.save(settings);
        return NotificationSettingsResponse.builder()
                .id(settings.getId())
                .orderUpdates(settings.getEnableOrderUpdates())
                .paymentConfirmation(settings.getEnablePaymentConfirmation())
                .shippingUpdates(settings.getEnableShippingUpdates())
                .promotionalEmails(settings.getEnablePromotionalEmails())
                .newProductAlerts(settings.getEnableNewProductAlerts())
                .priceDropAlerts(settings.getEnablePriceDropAlerts())
                .wishlistUpdates(settings.getEnableWishlistUpdates())
                .reviewRequests(settings.getEnableReviewRequests())
                .newsletter(settings.getEnableNewsletter())
                .promotionalSms(settings.getEnablePromotionalSms())
                .browserNotifications(settings.getEnableBrowserNotifications())
                .appNotifications(settings.getEnableAppNotifications())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
    @Cacheable(value = "settings", key = "'security'")
    @Transactional(readOnly = true)
    public SecuritySettingsResponse getSecuritySettings() {
        SiteSettings settings = getOrCreateSettings();
        return SecuritySettingsResponse.builder()
                .id(settings.getId())
                .twoFactorEnabled(settings.getTwoFactorEnabled())
                .sessionTimeoutMinutes(settings.getSessionTimeoutMinutes())
                .loginNotifications(settings.getLoginNotifications())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public SecuritySettingsResponse updateSecuritySettings(SecuritySettingsRequest request) {
        SiteSettings settings = getOrCreateSettings();
        settings.setTwoFactorEnabled(request.getTwoFactorEnabled());
        settings.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        settings.setLoginNotifications(request.getLoginNotifications());
        settings = siteSettingsRepository.save(settings);
        return SecuritySettingsResponse.builder()
                .id(settings.getId())
                .twoFactorEnabled(settings.getTwoFactorEnabled())
                .sessionTimeoutMinutes(settings.getSessionTimeoutMinutes())
                .loginNotifications(settings.getLoginNotifications())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @Cacheable(value = "settings", key = "'social'")
    @Transactional(readOnly = true)
    public SocialLinksResponse getSocialLinks() {
        SocialLinks links = socialLinksRepository.findAll().stream().findFirst()
                .orElseGet(() -> socialLinksRepository.save(SocialLinks.builder().build()));
        return SocialLinksResponse.from(links);
    }

    @CacheEvict(value = "settings", allEntries = true)
    @Transactional
    public SocialLinksResponse updateSocialLinks(SocialLinksRequest request) {
        SocialLinks links = socialLinksRepository.findAll().stream().findFirst()
                .orElseGet(() -> SocialLinks.builder().build());
        links.setFacebookUrl(request.getFacebookUrl());
        links.setTwitterUrl(request.getTwitterUrl());
        links.setInstagramUrl(request.getInstagramUrl());
        links.setLinkedinUrl(request.getLinkedinUrl());
        links.setYoutubeUrl(request.getYoutubeUrl());
        links.setTiktokUrl(request.getTiktokUrl());
        links.setPinterestUrl(request.getPinterestUrl());
        links.setWhatsappNumber(request.getWhatsappNumber());
        return SocialLinksResponse.from(socialLinksRepository.save(links));
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 12) return key;
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }
}

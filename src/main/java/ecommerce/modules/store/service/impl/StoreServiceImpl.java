package ecommerce.modules.store.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.notification.entity.SellerNotificationSettings;
import ecommerce.modules.notification.repository.SellerNotificationSettingsRepository;
import ecommerce.modules.store.dto.*;
import ecommerce.modules.store.entity.ShippingZone;
import ecommerce.modules.store.repository.ShippingZoneRepository;
import ecommerce.modules.store.service.StoreService;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final SellerProfileRepository sellerProfileRepository;
    private final ShippingZoneRepository shippingZoneRepository;
    private final SellerNotificationSettingsRepository sellerNotificationSettingsRepository;

    @Override
    public StoreResponse getStore(UUID sellerId) {
        return mapToStoreResponse(findProfile(sellerId));
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID sellerId, UpdateStoreRequest request) {
        SellerProfile profile = findProfile(sellerId);
        if (request.getStoreName() != null) profile.setStoreName(request.getStoreName());
        if (request.getStoreDescription() != null) profile.setStoreDescription(request.getStoreDescription());
        if (request.getStoreWebsite() != null) profile.setStoreWebsite(request.getStoreWebsite());
        if (request.getStoreLogo() != null) profile.setStoreLogo(request.getStoreLogo());
        if (request.getStoreBanner() != null) profile.setStoreBanner(request.getStoreBanner());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getRegion() != null) profile.setRegion(request.getRegion());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getBusinessAddress() != null) profile.setBusinessAddress(request.getBusinessAddress());
        if (request.getWorkingHours() != null) profile.setWorkingHours(request.getWorkingHours());
        if (request.getFacebookUrl() != null) profile.setFacebookUrl(request.getFacebookUrl());
        if (request.getInstagramUrl() != null) profile.setInstagramUrl(request.getInstagramUrl());
        if (request.getTwitterUrl() != null) profile.setTwitterUrl(request.getTwitterUrl());
        if (request.getBusinessRegistration() != null) profile.setBusinessRegistration(request.getBusinessRegistration());
        if (request.getBankName() != null) profile.setBankName(request.getBankName());
        if (request.getAccountHolderName() != null) profile.setAccountHolderName(request.getAccountHolderName());
        if (request.getAccountNumber() != null) profile.setAccountNumber(request.getAccountNumber());
        return mapToStoreResponse(sellerProfileRepository.save(profile));
    }

    @Override
    public SellerPaymentSettingsResponse getPaymentSettings(UUID sellerId) {
        SellerProfile profile = findProfile(sellerId);
        return SellerPaymentSettingsResponse.builder()
                .id(profile.getPublicId())
                .bankName(profile.getBankName())
                .accountHolderName(profile.getAccountHolderName())
                .accountNumber(maskAccountNumber(profile.getAccountNumber()))
                .branch(profile.getBranch())
                .payoutSchedule(profile.getPayoutSchedule())
                .updatedAt(toLocalDateTime(profile))
                .build();
    }

    @Override
    @Transactional
    public SellerPaymentSettingsResponse updatePaymentSettings(UUID sellerId, SellerPaymentSettingsRequest request) {
        SellerProfile profile = findProfile(sellerId);
        if (request.getBankName() != null) profile.setBankName(request.getBankName());
        if (request.getAccountHolderName() != null) profile.setAccountHolderName(request.getAccountHolderName());
        if (request.getAccountNumber() != null) profile.setAccountNumber(request.getAccountNumber());
        if (request.getBranch() != null) profile.setBranch(request.getBranch());
        if (request.getPayoutSchedule() != null) profile.setPayoutSchedule(request.getPayoutSchedule());
        SellerProfile saved = sellerProfileRepository.save(profile);
        log.info("Updated payment settings for seller: {}", sellerId);
        return SellerPaymentSettingsResponse.builder()
                .id(saved.getPublicId())
                .bankName(saved.getBankName())
                .accountHolderName(saved.getAccountHolderName())
                .accountNumber(maskAccountNumber(saved.getAccountNumber()))
                .branch(saved.getBranch())
                .payoutSchedule(saved.getPayoutSchedule())
                .updatedAt(toLocalDateTime(saved))
                .build();
    }

    @Override
    public SellerShippingSettingsResponse getShippingSettings(UUID sellerId) {
        SellerProfile profile = findProfile(sellerId);
        List<ShippingZoneResponse> zones = shippingZoneRepository.findBySellerIdAndIsActiveTrue(profile.getId())
                .stream().map(this::mapToShippingZoneResponse).collect(Collectors.toList());
        return SellerShippingSettingsResponse.builder()
                .id(profile.getPublicId())
                .returnPolicy(profile.getReturnPolicy())
                .shippingZones(zones)
                .updatedAt(toLocalDateTime(profile))
                .build();
    }

    @Override
    @Transactional
    public SellerShippingSettingsResponse updateShippingSettings(UUID sellerId, SellerShippingSettingsRequest request) {
        SellerProfile profile = findProfile(sellerId);
        if (request.getReturnPolicy() != null) profile.setReturnPolicy(request.getReturnPolicy());
        SellerProfile saved = sellerProfileRepository.save(profile);
        log.info("Updated shipping settings for seller: {}", sellerId);
        List<ShippingZoneResponse> zones = shippingZoneRepository.findBySellerIdAndIsActiveTrue(saved.getId())
                .stream().map(this::mapToShippingZoneResponse).collect(Collectors.toList());
        return SellerShippingSettingsResponse.builder()
                .id(saved.getPublicId())
                .returnPolicy(saved.getReturnPolicy())
                .shippingZones(zones)
                .updatedAt(toLocalDateTime(saved))
                .build();
    }

    @Override
    public List<ShippingZoneResponse> getShippingZones(UUID sellerId) {
        SellerProfile profile = findProfile(sellerId);
        return shippingZoneRepository.findBySellerId(profile.getId())
                .stream().map(this::mapToShippingZoneResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShippingZoneResponse createShippingZone(UUID sellerId, ShippingZoneRequest request) {
        SellerProfile profile = findProfile(sellerId);
        ShippingZone zone = ShippingZone.builder()
                .seller(profile)
                .zoneName(request.getZoneName())
                .zoneDescription(request.getZoneDescription())
                .region(request.getRegion())
                .deliveryMethod(request.getDeliveryMethod())
                .shippingCost(request.getShippingCost())
                .freeShippingMin(request.getFreeShippingMin())
                .estimatedDays(request.getEstimatedDays())
                .isActive(true)
                .build();
        ShippingZone saved = shippingZoneRepository.save(zone);
        log.info("Created shipping zone {} for seller: {}", saved.getId(), sellerId);
        return mapToShippingZoneResponse(saved);
    }

    @Override
    @Transactional
    public ShippingZoneResponse updateShippingZone(UUID sellerId, UUID zoneId, ShippingZoneRequest request) {
        SellerProfile profile = findProfile(sellerId);
        ShippingZone zone = shippingZoneRepository.findByPublicId(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found"));
        if (!zone.getSeller().getId().equals(profile.getId())) {
            throw new BadRequestException("Shipping zone does not belong to this seller");
        }
        if (request.getZoneName() != null) zone.setZoneName(request.getZoneName());
        if (request.getZoneDescription() != null) zone.setZoneDescription(request.getZoneDescription());
        if (request.getRegion() != null) zone.setRegion(request.getRegion());
        if (request.getDeliveryMethod() != null) zone.setDeliveryMethod(request.getDeliveryMethod());
        if (request.getShippingCost() != null) zone.setShippingCost(request.getShippingCost());
        if (request.getFreeShippingMin() != null) zone.setFreeShippingMin(request.getFreeShippingMin());
        if (request.getEstimatedDays() != null) zone.setEstimatedDays(request.getEstimatedDays());
        log.info("Updated shipping zone {} for seller: {}", zoneId, sellerId);
        return mapToShippingZoneResponse(shippingZoneRepository.save(zone));
    }

    @Override
    @Transactional
    public void deleteShippingZone(UUID sellerId, UUID zoneId) {
        SellerProfile profile = findProfile(sellerId);
        ShippingZone zone = shippingZoneRepository.findByPublicId(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found"));
        if (!zone.getSeller().getId().equals(profile.getId())) {
            throw new BadRequestException("Shipping zone does not belong to this seller");
        }
        zone.setIsActive(false);
        shippingZoneRepository.save(zone);
        log.info("Deleted shipping zone {} for seller: {}", zoneId, sellerId);
    }

    @Override
    public SellerNotificationSettingsResponse getNotificationSettings(UUID sellerId) {
        SellerProfile profile = findProfile(sellerId);
        SellerNotificationSettings settings = sellerNotificationSettingsRepository.findBySellerId(profile.getId())
                .orElseGet(() -> createDefaultNotificationSettings(profile));
        return mapToNotificationResponse(settings);
    }

    @Override
    @Transactional
    public SellerNotificationSettingsResponse updateNotificationSettings(UUID sellerId, SellerNotificationSettingsRequest request) {
        SellerProfile profile = findProfile(sellerId);
        SellerNotificationSettings settings = sellerNotificationSettingsRepository.findBySellerId(profile.getId())
                .orElseGet(() -> createDefaultNotificationSettings(profile));
        if (request.getNewOrders() != null) settings.setNewOrders(request.getNewOrders());
        if (request.getOrderUpdates() != null) settings.setOrderUpdates(request.getOrderUpdates());
        if (request.getCustomerMessages() != null) settings.setCustomerMessages(request.getCustomerMessages());
        if (request.getStockAlerts() != null) settings.setStockAlerts(request.getStockAlerts());
        if (request.getPaymentUpdates() != null) settings.setPaymentUpdates(request.getPaymentUpdates());
        if (request.getRefundRequests() != null) settings.setRefundRequests(request.getRefundRequests());
        if (request.getPromotionalEmails() != null) settings.setPromotionalEmails(request.getPromotionalEmails());
        log.info("Updated notification settings for seller: {}", sellerId);
        return mapToNotificationResponse(sellerNotificationSettingsRepository.save(settings));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SellerProfile findProfile(UUID sellerId) {
        return sellerProfileRepository.findByUser_PublicId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
    }

    private StoreResponse mapToStoreResponse(SellerProfile p) {
        return StoreResponse.builder()
                .id(p.getPublicId())
                .storeName(p.getStoreName())
                .storeDescription(p.getStoreDescription())
                .storeWebsite(p.getStoreWebsite())
                .storeLogo(p.getStoreLogo())
                .storeBanner(p.getStoreBanner())
                .email(p.getEmail())
                .phone(p.getPhone())
                .region(p.getRegion())
                .city(p.getCity())
                .businessAddress(p.getBusinessAddress())
                .workingHours(p.getWorkingHours())
                .facebookUrl(p.getFacebookUrl())
                .instagramUrl(p.getInstagramUrl())
                .twitterUrl(p.getTwitterUrl())
                .rating(p.getRating())
                .totalReviews(p.getTotalReviews())
                .totalProducts(p.getTotalProducts())
                .totalSales(p.getTotalSales())
                .totalRevenue(p.getTotalRevenue())
                .verificationStatus(p.getVerificationStatus().name())
                .businessRegistration(p.getBusinessRegistration())
                .bankName(p.getBankName())
                .build();
    }

    private ShippingZoneResponse mapToShippingZoneResponse(ShippingZone z) {
        return ShippingZoneResponse.builder()
                .id(z.getPublicId())
                .zoneName(z.getZoneName())
                .zoneDescription(z.getZoneDescription())
                .region(z.getRegion())
                .deliveryMethod(z.getDeliveryMethod())
                .shippingCost(z.getShippingCost())
                .freeShippingMin(z.getFreeShippingMin())
                .estimatedDays(z.getEstimatedDays())
                .isActive(z.getIsActive())
                .updatedAt(z.getUpdatedAt() != null
                        ? LocalDateTime.ofInstant(z.getUpdatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    private SellerNotificationSettings createDefaultNotificationSettings(SellerProfile profile) {
        return sellerNotificationSettingsRepository.save(
                SellerNotificationSettings.builder()
                        .seller(profile)
                        .newOrders(true).orderUpdates(true).customerMessages(true)
                        .stockAlerts(true).paymentUpdates(true).refundRequests(true)
                        .promotionalEmails(false)
                        .build());
    }

    private SellerNotificationSettingsResponse mapToNotificationResponse(SellerNotificationSettings s) {
        return SellerNotificationSettingsResponse.builder()
                .id(s.getId())
                .newOrders(s.getNewOrders()).orderUpdates(s.getOrderUpdates())
                .customerMessages(s.getCustomerMessages()).stockAlerts(s.getStockAlerts())
                .paymentUpdates(s.getPaymentUpdates()).refundRequests(s.getRefundRequests())
                .promotionalEmails(s.getPromotionalEmails()).updatedAt(s.getUpdatedAt())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return accountNumber;
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private LocalDateTime toLocalDateTime(SellerProfile p) {
        return p.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(p.getUpdatedAt(), ZoneId.systemDefault()) : null;
    }
}

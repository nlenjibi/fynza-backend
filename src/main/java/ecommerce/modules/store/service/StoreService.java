package ecommerce.modules.store.service;

import ecommerce.modules.store.dto.*;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    StoreResponse getStore(UUID sellerId);

    StoreResponse updateStore(UUID sellerId, UpdateStoreRequest request);

    SellerPaymentSettingsResponse getPaymentSettings(UUID sellerId);

    SellerPaymentSettingsResponse updatePaymentSettings(UUID sellerId, SellerPaymentSettingsRequest request);

    SellerShippingSettingsResponse getShippingSettings(UUID sellerId);

    SellerShippingSettingsResponse updateShippingSettings(UUID sellerId, SellerShippingSettingsRequest request);

    List<ShippingZoneResponse> getShippingZones(UUID sellerId);

    ShippingZoneResponse createShippingZone(UUID sellerId, ShippingZoneRequest request);

    ShippingZoneResponse updateShippingZone(UUID sellerId, UUID zoneId, ShippingZoneRequest request);

    void deleteShippingZone(UUID sellerId, UUID zoneId);

    SellerNotificationSettingsResponse getNotificationSettings(UUID sellerId);

    SellerNotificationSettingsResponse updateNotificationSettings(UUID sellerId, SellerNotificationSettingsRequest request);
}

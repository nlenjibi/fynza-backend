package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.*;
import ecommerce.modules.shipping.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface CarrierAdminService {

    // Carriers
    CarrierResponse createCarrier(CreateCarrierRequest request);
    CarrierResponse getCarrier(UUID carrierPublicId);
    List<CarrierResponse> getAllCarriers();
    CarrierResponse toggleCarrier(UUID carrierPublicId, boolean active);

    // Shipping Methods
    ShippingMethodResponse createShippingMethod(CreateShippingMethodRequest request);
    List<ShippingMethodResponse> getMethodsByCarrier(UUID carrierPublicId);
    List<ShippingMethodResponse> getAllActiveMethods();
    ShippingMethodResponse toggleMethod(UUID methodPublicId, boolean active);

    // Shipping Zones
    ShippingZoneResponse createZone(CreateShippingZoneRequest request);
    List<ShippingZoneResponse> getAllZones();
    ShippingZoneResponse toggleZone(UUID zonePublicId, boolean active);

    // Shipping Rates
    ShippingRateResponse createRate(CreateShippingRateRequest request);
    List<ShippingRateResponse> getRatesByMethod(UUID methodPublicId);
    ShippingRateResponse toggleRate(UUID ratePublicId, boolean active);
}

package ecommerce.modules.shipping.service.impl;

import ecommerce.common.exception.ConflictException;
import ecommerce.modules.shipping.dto.request.*;
import ecommerce.modules.shipping.dto.response.*;
import ecommerce.modules.shipping.entity.*;
import ecommerce.modules.shipping.exception.CarrierNotFoundException;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.repository.*;
import ecommerce.modules.shipping.service.CarrierAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarrierAdminServiceImpl implements CarrierAdminService {

    private final CarrierRepository carrierRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingZoneRepository shippingZoneRepository;
    private final ShippingRateRepository shippingRateRepository;

    // =========================================================================
    // Carriers
    // =========================================================================

    @Override
    @Transactional
    public CarrierResponse createCarrier(CreateCarrierRequest request) {
        if (carrierRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new ConflictException("Carrier with code '" + request.getCode() + "' already exists");
        }
        Carrier carrier = Carrier.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .logoUrl(request.getLogoUrl())
                .trackingUrlTemplate(request.getTrackingUrlTemplate())
                .build();
        carrier = carrierRepository.save(carrier);
        log.info("Created carrier code={}", carrier.getCode());
        return CarrierResponse.from(carrier);
    }

    @Override
    public CarrierResponse getCarrier(UUID carrierPublicId) {
        return CarrierResponse.from(findCarrierOrThrow(carrierPublicId));
    }

    @Override
    public List<CarrierResponse> getAllCarriers() {
        return carrierRepository.findAll().stream().map(CarrierResponse::from).toList();
    }

    @Override
    @Transactional
    public CarrierResponse toggleCarrier(UUID carrierPublicId, boolean active) {
        Carrier carrier = findCarrierOrThrow(carrierPublicId);
        carrier.setIsActive(active);
        return CarrierResponse.from(carrierRepository.save(carrier));
    }

    // =========================================================================
    // Shipping Methods
    // =========================================================================

    @Override
    @Transactional
    public ShippingMethodResponse createShippingMethod(CreateShippingMethodRequest request) {
        Carrier carrier = findCarrierOrThrow(request.getCarrierId());
        if (shippingMethodRepository.existsByCarrier_IdAndCode(carrier.getId(), request.getCode())) {
            throw new ConflictException("Method with code '" + request.getCode() + "' already exists for this carrier");
        }
        ShippingMethod method = ShippingMethod.builder()
                .carrier(carrier)
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .estimatedDaysMin(request.getEstimatedDaysMin())
                .estimatedDaysMax(request.getEstimatedDaysMax())
                .build();
        method = shippingMethodRepository.save(method);
        log.info("Created shipping method code={} for carrier={}", method.getCode(), carrier.getCode());
        return ShippingMethodResponse.from(method);
    }

    @Override
    public List<ShippingMethodResponse> getMethodsByCarrier(UUID carrierPublicId) {
        return shippingMethodRepository.findByCarrier_PublicIdAndIsActiveTrue(carrierPublicId)
                .stream().map(ShippingMethodResponse::from).toList();
    }

    @Override
    public List<ShippingMethodResponse> getAllActiveMethods() {
        return shippingMethodRepository.findByIsActiveTrue()
                .stream().map(ShippingMethodResponse::from).toList();
    }

    @Override
    @Transactional
    public ShippingMethodResponse toggleMethod(UUID methodPublicId, boolean active) {
        ShippingMethod method = findMethodOrThrow(methodPublicId);
        method.setIsActive(active);
        return ShippingMethodResponse.from(shippingMethodRepository.save(method));
    }

    // =========================================================================
    // Shipping Zones
    // =========================================================================

    @Override
    @Transactional
    public ShippingZoneResponse createZone(CreateShippingZoneRequest request) {
        ShippingZone zone = ShippingZone.builder()
                .name(request.getName())
                .description(request.getDescription())
                .regions(request.getRegions())
                .build();
        zone = shippingZoneRepository.save(zone);
        log.info("Created shipping zone name={}", zone.getName());
        return ShippingZoneResponse.from(zone);
    }

    @Override
    public List<ShippingZoneResponse> getAllZones() {
        return shippingZoneRepository.findAll().stream().map(ShippingZoneResponse::from).toList();
    }

    @Override
    @Transactional
    public ShippingZoneResponse toggleZone(UUID zonePublicId, boolean active) {
        ShippingZone zone = findZoneOrThrow(zonePublicId);
        zone.setIsActive(active);
        return ShippingZoneResponse.from(shippingZoneRepository.save(zone));
    }

    // =========================================================================
    // Shipping Rates
    // =========================================================================

    @Override
    @Transactional
    public ShippingRateResponse createRate(CreateShippingRateRequest request) {
        ShippingMethod method = findMethodOrThrow(request.getShippingMethodId());
        ShippingZone zone = findZoneOrThrow(request.getZoneId());

        if (shippingRateRepository.existsByShippingMethod_IdAndZone_Id(method.getId(), zone.getId())) {
            throw new ConflictException("A rate for this method and zone already exists");
        }

        ShippingRate rate = ShippingRate.builder()
                .shippingMethod(method)
                .zone(zone)
                .baseFee(request.getBaseFee())
                .perKgFee(request.getPerKgFee())
                .freeShippingThreshold(request.getFreeShippingThreshold())
                .build();
        rate = shippingRateRepository.save(rate);
        log.info("Created shipping rate for method={} zone={}", method.getCode(), zone.getName());
        return ShippingRateResponse.from(rate);
    }

    @Override
    public List<ShippingRateResponse> getRatesByMethod(UUID methodPublicId) {
        return shippingRateRepository.findByShippingMethod_PublicId(methodPublicId)
                .stream().map(ShippingRateResponse::from).toList();
    }

    @Override
    @Transactional
    public ShippingRateResponse toggleRate(UUID ratePublicId, boolean active) {
        ShippingRate rate = shippingRateRepository.findByPublicId(ratePublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipping rate not found: " + ratePublicId));
        rate.setIsActive(active);
        return ShippingRateResponse.from(shippingRateRepository.save(rate));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Carrier findCarrierOrThrow(UUID publicId) {
        return carrierRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CarrierNotFoundException("Carrier not found: " + publicId));
    }

    private ShippingMethod findMethodOrThrow(UUID publicId) {
        return shippingMethodRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CarrierNotFoundException("Shipping method not found: " + publicId));
    }

    private ShippingZone findZoneOrThrow(UUID publicId) {
        return shippingZoneRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CarrierNotFoundException("Shipping zone not found: " + publicId));
    }
}

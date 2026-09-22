package ecommerce.modules.shipping.service.impl;

import ecommerce.modules.shipping.dto.request.CalculateShippingRateRequest;
import ecommerce.modules.shipping.dto.response.ShippingRateResponse;
import ecommerce.modules.shipping.repository.ShippingRateRepository;
import ecommerce.modules.shipping.service.ShippingRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingRateServiceImpl implements ShippingRateService {

    private final ShippingRateRepository shippingRateRepository;

    @Override
    public List<ShippingRateResponse> calculateRates(CalculateShippingRateRequest request) {
        List<ShippingRateResponse> rates = shippingRateRepository
                .findActiveRatesForRegion(request.getDestinationRegion())
                .stream()
                .map(rate -> {
                    // Apply free-shipping threshold — return zero-cost rate if eligible
                    if (rate.getFreeShippingThreshold() != null
                            && request.getOrderTotal() != null
                            && request.getOrderTotal().compareTo(rate.getFreeShippingThreshold()) >= 0) {
                        return ShippingRateResponse.builder()
                                .id(rate.getPublicId())
                                .shippingMethodId(rate.getShippingMethod().getPublicId())
                                .shippingMethodName(rate.getShippingMethod().getName())
                                .carrierName(rate.getShippingMethod().getCarrier().getName())
                                .estimatedDaysMin(rate.getShippingMethod().getEstimatedDaysMin())
                                .estimatedDaysMax(rate.getShippingMethod().getEstimatedDaysMax())
                                .zoneId(rate.getZone().getPublicId())
                                .zoneName(rate.getZone().getName())
                                .baseFee(BigDecimal.ZERO)
                                .perKgFee(BigDecimal.ZERO)
                                .freeShippingThreshold(rate.getFreeShippingThreshold())
                                .currency(rate.getCurrency().name())
                                .isActive(true)
                                .build();
                    }
                    return ShippingRateResponse.from(rate);
                })
                .toList();
        return rates;
    }

    @Override
    public List<ShippingRateResponse> getAllActiveRates() {
        return shippingRateRepository.findActiveRatesForRegion("%")
                .stream().map(ShippingRateResponse::from).toList();
    }
}

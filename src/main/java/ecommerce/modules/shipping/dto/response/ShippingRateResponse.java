package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShippingRate;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class ShippingRateResponse {
    UUID id;
    UUID shippingMethodId;
    String shippingMethodName;
    String carrierName;
    Integer estimatedDaysMin;
    Integer estimatedDaysMax;
    UUID zoneId;
    String zoneName;
    BigDecimal baseFee;
    BigDecimal perKgFee;
    BigDecimal freeShippingThreshold;
    String currency;
    Boolean isActive;

    public static ShippingRateResponse from(ShippingRate rate) {
        return ShippingRateResponse.builder()
                .id(rate.getPublicId())
                .shippingMethodId(rate.getShippingMethod().getPublicId())
                .shippingMethodName(rate.getShippingMethod().getName())
                .carrierName(rate.getShippingMethod().getCarrier().getName())
                .estimatedDaysMin(rate.getShippingMethod().getEstimatedDaysMin())
                .estimatedDaysMax(rate.getShippingMethod().getEstimatedDaysMax())
                .zoneId(rate.getZone().getPublicId())
                .zoneName(rate.getZone().getName())
                .baseFee(rate.getBaseFee())
                .perKgFee(rate.getPerKgFee())
                .freeShippingThreshold(rate.getFreeShippingThreshold())
                .currency(rate.getCurrency().name())
                .isActive(rate.getIsActive())
                .build();
    }
}

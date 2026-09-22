package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShippingMethod;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ShippingMethodResponse {
    UUID id;
    UUID carrierId;
    String carrierName;
    String carrierCode;
    String name;
    String code;
    String description;
    Integer estimatedDaysMin;
    Integer estimatedDaysMax;
    Boolean isActive;
    Instant createdAt;

    public static ShippingMethodResponse from(ShippingMethod method) {
        return ShippingMethodResponse.builder()
                .id(method.getPublicId())
                .carrierId(method.getCarrier().getPublicId())
                .carrierName(method.getCarrier().getName())
                .carrierCode(method.getCarrier().getCode())
                .name(method.getName())
                .code(method.getCode())
                .description(method.getDescription())
                .estimatedDaysMin(method.getEstimatedDaysMin())
                .estimatedDaysMax(method.getEstimatedDaysMax())
                .isActive(method.getIsActive())
                .createdAt(method.getCreatedAt())
                .build();
    }
}

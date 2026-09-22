package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.Carrier;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CarrierResponse {
    UUID id;
    String name;
    String code;
    String logoUrl;
    String trackingUrlTemplate;
    Boolean isActive;
    Instant createdAt;

    public static CarrierResponse from(Carrier carrier) {
        return CarrierResponse.builder()
                .id(carrier.getPublicId())
                .name(carrier.getName())
                .code(carrier.getCode())
                .logoUrl(carrier.getLogoUrl())
                .trackingUrlTemplate(carrier.getTrackingUrlTemplate())
                .isActive(carrier.getIsActive())
                .createdAt(carrier.getCreatedAt())
                .build();
    }
}

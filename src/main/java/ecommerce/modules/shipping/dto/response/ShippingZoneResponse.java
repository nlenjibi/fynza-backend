package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShippingZone;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ShippingZoneResponse {
    UUID id;
    String name;
    String description;
    List<String> regions;
    Boolean isActive;
    Instant createdAt;

    public static ShippingZoneResponse from(ShippingZone zone) {
        return ShippingZoneResponse.builder()
                .id(zone.getPublicId())
                .name(zone.getName())
                .description(zone.getDescription())
                .regions(zone.getRegions())
                .isActive(zone.getIsActive())
                .createdAt(zone.getCreatedAt())
                .build();
    }
}

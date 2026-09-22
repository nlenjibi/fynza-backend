package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.TrackingEvent;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class TrackingEventResponse {
    UUID id;
    ShipmentStatus status;
    String description;
    String location;
    Instant occurredAt;

    public static TrackingEventResponse from(TrackingEvent event) {
        return TrackingEventResponse.builder()
                .id(event.getPublicId())
                .status(event.getStatus())
                .description(event.getDescription())
                .location(event.getLocation())
                .occurredAt(event.getOccurredAt())
                .build();
    }
}

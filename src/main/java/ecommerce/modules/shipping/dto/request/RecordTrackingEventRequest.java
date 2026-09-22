package ecommerce.modules.shipping.dto.request;

import ecommerce.modules.shipping.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class RecordTrackingEventRequest {

    @NotNull(message = "Status is required")
    private ShipmentStatus status;

    private String description;
    private String location;
    private Instant occurredAt;
}

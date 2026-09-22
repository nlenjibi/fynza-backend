package ecommerce.modules.shipping.dto.request;

import ecommerce.modules.shipping.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateShipmentStatusRequest {

    @NotNull(message = "Status is required")
    private ShipmentStatus status;

    private String trackingNumber;
    private String location;
    private String notes;
}

package ecommerce.modules.refund.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateReturnShipmentRequest {

    /** Public ID of the Shipment created in the Shipping module for this return */
    @NotNull(message = "Return shipment ID is required")
    private UUID returnShipmentId;

    /** Label reference (URL/key) provided by the shipping carrier or platform */
    private String returnLabelReference;
}

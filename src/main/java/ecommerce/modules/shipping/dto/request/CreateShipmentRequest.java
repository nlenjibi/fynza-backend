package ecommerce.modules.shipping.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateShipmentRequest {

    @NotNull(message = "Fulfillment ID is required")
    private UUID fulfillmentId;

    private UUID carrierId;

    private UUID shippingMethodId;

    @NotNull(message = "At least one item is required")
    @Valid
    private List<ShipmentItemRequest> items;

    private ShipmentAddressRequest address;

    @Positive
    private BigDecimal weightKg;

    @Positive
    private BigDecimal lengthCm;

    @Positive
    private BigDecimal widthCm;

    @Positive
    private BigDecimal heightCm;

    private BigDecimal shippingCost;

    private LocalDate estimatedDeliveryDate;
}

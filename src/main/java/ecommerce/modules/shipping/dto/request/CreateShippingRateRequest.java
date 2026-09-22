package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateShippingRateRequest {

    @NotNull(message = "Shipping method ID is required")
    private UUID shippingMethodId;

    @NotNull(message = "Zone ID is required")
    private UUID zoneId;

    @DecimalMin("0.00")
    private BigDecimal baseFee = BigDecimal.ZERO;

    @DecimalMin("0.00")
    private BigDecimal perKgFee = BigDecimal.ZERO;

    private BigDecimal freeShippingThreshold;
}

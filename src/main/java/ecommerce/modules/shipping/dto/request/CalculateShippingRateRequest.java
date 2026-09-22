package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculateShippingRateRequest {

    @NotBlank(message = "Destination region is required")
    private String destinationRegion;

    @Positive
    private BigDecimal weightKg;

    private BigDecimal orderTotal;
}

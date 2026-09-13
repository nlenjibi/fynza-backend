package ecommerce.modules.pricing.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceOverrideRequest {

    @NotNull(message = "newAmount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "newAmount must be >= 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal newAmount;

    @NotBlank(message = "reason is required")
    private String reason;
}

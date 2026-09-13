package ecommerce.modules.pricing.dto.request;

import ecommerce.modules.pricing.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePriceTierRequest {

    @NotNull(message = "priceId is required")
    private UUID priceId;

    @NotNull(message = "minQuantity is required")
    @Min(value = 1, message = "minQuantity must be at least 1")
    private Integer minQuantity;

    @Min(value = 1, message = "maxQuantity must be at least 1")
    private Integer maxQuantity;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "unitPrice must be >= 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal unitPrice;

    @NotNull(message = "currency is required")
    private SupportedCurrency currency;
}

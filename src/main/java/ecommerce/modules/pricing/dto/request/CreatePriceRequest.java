package ecommerce.modules.pricing.dto.request;

import ecommerce.modules.pricing.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePriceRequest {

    @NotNull(message = "productId is required")
    private UUID productId;

    private UUID variantId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "amount must be >= 0")
    @Digits(integer = 15, fraction = 4, message = "amount precision is at most 15 integer digits and 4 decimal places")
    private BigDecimal amount;

    @DecimalMin(value = "0.0", inclusive = true, message = "saleAmount must be >= 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal saleAmount;

    @NotNull(message = "currency is required")
    private SupportedCurrency currency;

    private UUID priceListId;

    private Instant validFrom;

    private Instant validUntil;
}

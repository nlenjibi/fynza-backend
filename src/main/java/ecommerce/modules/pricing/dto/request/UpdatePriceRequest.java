package ecommerce.modules.pricing.dto.request;

import ecommerce.modules.pricing.enums.SupportedCurrency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePriceRequest {

    @DecimalMin(value = "0.0", inclusive = true, message = "amount must be >= 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @DecimalMin(value = "0.0", inclusive = true, message = "saleAmount must be >= 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal saleAmount;

    private SupportedCurrency currency;

    private Instant validFrom;

    private Instant validUntil;

    private String reason;
}

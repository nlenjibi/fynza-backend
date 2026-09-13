package ecommerce.modules.pricing.dto.response;

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
public class PriceResultResponse {

    private UUID priceId;
    private UUID productId;
    private UUID variantId;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal effectivePrice;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private String currency;
    private Instant validFrom;
    private Instant validUntil;
}

package ecommerce.modules.pricing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceResponse {

    private UUID publicId;
    private UUID productId;
    private UUID variantId;
    private BigDecimal amount;
    private BigDecimal saleAmount;
    private String currency;
    private String status;
    private Instant validFrom;
    private Instant validUntil;
    private Boolean isActive;
    private List<PriceTierResponse> tiers;
    private Instant createdAt;
    private Instant updatedAt;
}

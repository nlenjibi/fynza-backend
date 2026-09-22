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
public class PriceTierResponse {

    private UUID publicId;
    private Integer minQuantity;
    private Integer maxQuantity;
    private BigDecimal unitPrice;
    private String currency;
    private Boolean isActive;
    private Instant createdAt;
}

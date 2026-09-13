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
public class PriceHistoryResponse {

    private Long id;
    private BigDecimal oldAmount;
    private BigDecimal newAmount;
    private String oldCurrency;
    private String newCurrency;
    private String oldStatus;
    private String newStatus;
    private UUID changedBy;
    private String reason;
    private Instant createdAt;
}

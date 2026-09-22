package ecommerce.modules.payout.dto.response;

import ecommerce.modules.payout.enums.LedgerDirection;
import ecommerce.modules.payout.enums.LedgerEntryType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LedgerEntryResponse {
    private UUID id;
    private LedgerEntryType entryType;
    private LedgerDirection direction;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private Instant createdAt;
}

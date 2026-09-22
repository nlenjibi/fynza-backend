package ecommerce.modules.payout.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID publicId;
    private BigDecimal pendingBalance;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    private BigDecimal totalEarned;
    private BigDecimal totalPaidOut;
    private String currency;
}

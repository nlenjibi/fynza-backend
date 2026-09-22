package ecommerce.modules.payout.dto.response;

import ecommerce.modules.payout.enums.PayoutAccountStatus;
import ecommerce.modules.payout.enums.PayoutAccountType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PayoutAccountResponse {
    private UUID id;
    private PayoutAccountType type;
    private String provider;
    private String accountName;
    private String accountNumber;
    private String country;
    private String currency;
    private Boolean isDefault;
    private Boolean isVerified;
    private PayoutAccountStatus status;
    private Instant verifiedAt;
}

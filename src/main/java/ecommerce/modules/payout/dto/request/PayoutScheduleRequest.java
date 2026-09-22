package ecommerce.modules.payout.dto.request;

import ecommerce.modules.payout.enums.PayoutFrequency;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PayoutScheduleRequest {
    private PayoutFrequency frequency;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private BigDecimal minimumAmount;
    private UUID payoutAccountId;
    private Boolean enabled;
}

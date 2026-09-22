package ecommerce.modules.payout.dto.request;

import lombok.Data;

@Data
public class UpdatePayoutAccountRequest {
    private String accountName;
    private Boolean isDefault;
}

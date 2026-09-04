package ecommerce.modules.seller.dto;

import ecommerce.common.enums.PayoutSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPaymentSettingsRequest {
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branch;
    private PayoutSchedule payoutSchedule;
}

package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerPaymentSettingsInput {
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branch;
    private String payoutSchedule;
}

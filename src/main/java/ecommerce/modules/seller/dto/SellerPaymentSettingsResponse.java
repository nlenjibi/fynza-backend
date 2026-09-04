package ecommerce.modules.seller.dto;

import ecommerce.common.enums.PayoutSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPaymentSettingsResponse {
    private UUID id;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branch;
    private PayoutSchedule payoutSchedule;
    private LocalDateTime updatedAt;
}

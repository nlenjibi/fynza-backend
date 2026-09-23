package ecommerce.modules.refund.dto;

import ecommerce.modules.refund.enums.ReturnReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnEligibilityResult {

    private boolean eligible;
    private Instant deadline;
    private Long remainingDays;
    private String reason;
    private List<ReturnReason> allowedReasons;
    private Integer maximumReturnQuantity;
    private List<String> conditions;
    private List<String> warnings;
}

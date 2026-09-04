package ecommerce.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyRedemptionRequest {
    
    private Integer pointsToRedeem;
    
    private String rewardType;
    
    private BigDecimal estimatedValue;
}

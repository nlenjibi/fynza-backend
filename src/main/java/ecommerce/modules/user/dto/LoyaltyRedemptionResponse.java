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
public class LoyaltyRedemptionResponse {
    
    private Integer previousPoints;
    
    private Integer redeemedPoints;
    
    private Integer remainingPoints;
    
    private BigDecimal discountAmount;
    
    private String rewardType;
    
    private String couponCode;
    
    private String message;
}

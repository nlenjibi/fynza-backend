package ecommerce.modules.order.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentResult {

    private UUID fulfillmentCenterId;
    private String fulfillmentCenterName;
    private BigDecimal score;
    private BigDecimal estimatedShippingCost;
    private int estimatedDeliveryDays;
    private String reason;

    public static FulfillmentResult fallback(String reason) {
        return FulfillmentResult.builder()
                .fulfillmentCenterId(UUID.randomUUID())
                .fulfillmentCenterName("DEFAULT")
                .score(BigDecimal.ZERO)
                .reason(reason)
                .build();
    }
}

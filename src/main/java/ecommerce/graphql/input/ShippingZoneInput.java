package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingZoneInput {
    private String zoneName;
    private String zoneDescription;
    private String region;
    private String deliveryMethod;
    private BigDecimal shippingCost;
    private BigDecimal freeShippingMin;
    private String estimatedDays;
}

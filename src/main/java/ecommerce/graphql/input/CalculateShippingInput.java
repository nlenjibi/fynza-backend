package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CalculateShippingInput {
    private UUID zoneId;
    private UUID regionId;
    private BigDecimal weight;
    private BigDecimal subtotal;
}

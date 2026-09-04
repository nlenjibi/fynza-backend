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
public class RefundRequestInput {
    private UUID orderId;
    private String reason;
    private String reasonDescription;
    private BigDecimal refundAmount;
}

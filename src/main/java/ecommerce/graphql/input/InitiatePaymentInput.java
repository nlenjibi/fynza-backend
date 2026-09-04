package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InitiatePaymentInput {
    private UUID orderId;
    private UUID paymentMethodId;
    private String paymentMethod;
    private Boolean savePaymentMethod;
}

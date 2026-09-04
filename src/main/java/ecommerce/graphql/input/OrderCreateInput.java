package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateInput {
    private UUID shippingAddressId;
    private UUID billingAddressId;
    private String paymentMethod;
    private String couponCode;
    private List<OrderItemInput> items;
    private BigDecimal shippingCost;
    private String customerNotes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInput {
        private UUID productId;
        private int quantity;
    }
}

package ecommerce.modules.order.dto;

import ecommerce.modules.order.entity.OrderAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private UUID customerId;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String couponCode;
    private String trackingNumber;
    private String customerNotes;
    private OrderAddress shippingAddress;
    private OrderAddress billingAddress;
    private Instant createdAt;
    private Instant updatedAt;
}

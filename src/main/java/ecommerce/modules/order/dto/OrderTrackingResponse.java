package ecommerce.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for order tracking information.
 * 
 * Contains comprehensive tracking data including:
 * - Basic order info (ID, order number, status)
 * - Tracking details (tracking number, estimated delivery)
 * - Shipping address
 * - Timeline of events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingResponse {
    private UUID orderId;
    private String orderNumber;
    private String status;
    private String displayName;
    private String trackingNumber;
    private LocalDateTime estimatedDelivery;
    private String shippingAddress;
    private List<OrderTimelineResponse> timeline;
}

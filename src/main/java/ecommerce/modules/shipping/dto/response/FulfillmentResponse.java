package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.Fulfillment;
import ecommerce.modules.shipping.enums.FulfillmentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class FulfillmentResponse {
    UUID id;
    UUID orderId;
    UUID sellerOrderId;
    FulfillmentStatus status;
    String notes;
    List<ShipmentResponse> shipments;
    Instant packedAt;
    Instant shippedAt;
    Instant completedAt;
    Instant cancelledAt;
    Instant createdAt;
    Instant updatedAt;

    public static FulfillmentResponse from(Fulfillment fulfillment, List<ShipmentResponse> shipments) {
        return FulfillmentResponse.builder()
                .id(fulfillment.getPublicId())
                .orderId(fulfillment.getOrderId())
                .sellerOrderId(fulfillment.getSellerOrderId())
                .status(fulfillment.getStatus())
                .notes(fulfillment.getNotes())
                .shipments(shipments)
                .packedAt(fulfillment.getPackedAt())
                .shippedAt(fulfillment.getShippedAt())
                .completedAt(fulfillment.getCompletedAt())
                .cancelledAt(fulfillment.getCancelledAt())
                .createdAt(fulfillment.getCreatedAt())
                .updatedAt(fulfillment.getUpdatedAt())
                .build();
    }
}

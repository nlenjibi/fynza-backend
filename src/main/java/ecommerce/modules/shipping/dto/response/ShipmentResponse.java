package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ShipmentResponse {
    UUID id;
    String shipmentNumber;
    UUID fulfillmentId;
    UUID carrierId;
    String carrierName;
    UUID shippingMethodId;
    String shippingMethodName;
    ShipmentStatus status;
    String trackingNumber;
    String labelUrl;
    LocalDate estimatedDeliveryDate;
    LocalDate actualDeliveryDate;
    ShipmentAddressResponse address;
    BigDecimal weightKg;
    BigDecimal shippingCost;
    String currency;
    List<ShipmentItemResponse> items;
    List<TrackingEventResponse> trackingEvents;
    Instant labelCreatedAt;
    Instant pickedUpAt;
    Instant deliveredAt;
    Instant cancelledAt;
    Instant createdAt;
    Instant updatedAt;

    public static ShipmentResponse from(Shipment shipment,
                                        List<ShipmentItemResponse> items,
                                        List<TrackingEventResponse> trackingEvents) {
        return ShipmentResponse.builder()
                .id(shipment.getPublicId())
                .shipmentNumber(shipment.getShipmentNumber())
                .fulfillmentId(shipment.getFulfillment().getPublicId())
                .carrierId(shipment.getCarrier() != null ? shipment.getCarrier().getPublicId() : null)
                .carrierName(shipment.getCarrier() != null ? shipment.getCarrier().getName() : null)
                .shippingMethodId(shipment.getShippingMethod() != null ? shipment.getShippingMethod().getPublicId() : null)
                .shippingMethodName(shipment.getShippingMethod() != null ? shipment.getShippingMethod().getName() : null)
                .status(shipment.getStatus())
                .trackingNumber(shipment.getTrackingNumber())
                .labelUrl(shipment.getLabelUrl())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .address(ShipmentAddressResponse.from(shipment.getAddress()))
                .weightKg(shipment.getWeightKg())
                .shippingCost(shipment.getShippingCost())
                .currency(shipment.getCurrency().name())
                .items(items)
                .trackingEvents(trackingEvents)
                .labelCreatedAt(shipment.getLabelCreatedAt())
                .pickedUpAt(shipment.getPickedUpAt())
                .deliveredAt(shipment.getDeliveredAt())
                .cancelledAt(shipment.getCancelledAt())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}

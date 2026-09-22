package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.CreateShipmentRequest;
import ecommerce.modules.shipping.dto.request.RecordTrackingEventRequest;
import ecommerce.modules.shipping.dto.request.UpdateShipmentStatusRequest;
import ecommerce.modules.shipping.dto.response.ShipmentResponse;
import ecommerce.modules.shipping.dto.response.TrackingEventResponse;

import java.util.List;
import java.util.UUID;

public interface ShipmentService {

    ShipmentResponse createShipment(UUID userId, CreateShipmentRequest request);

    ShipmentResponse getShipment(UUID shipmentPublicId);

    ShipmentResponse getByTrackingNumber(String trackingNumber);

    List<ShipmentResponse> getShipmentsForOrder(UUID orderId);

    ShipmentResponse updateStatus(UUID userId, UUID shipmentPublicId, UpdateShipmentStatusRequest request);

    ShipmentResponse generateLabel(UUID userId, UUID shipmentPublicId);

    TrackingEventResponse recordTrackingEvent(UUID shipmentPublicId, RecordTrackingEventRequest request);

    void cancelShipment(UUID userId, UUID shipmentPublicId);
}

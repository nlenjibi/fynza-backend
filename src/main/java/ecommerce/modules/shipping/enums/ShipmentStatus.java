package ecommerce.modules.shipping.enums;

public enum ShipmentStatus {
    DRAFT,
    READY,
    LABEL_CREATED,
    PICKUP_SCHEDULED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURN_TO_SENDER,
    RETURNED,
    CANCELLED,
    LOST,
    DAMAGED
}

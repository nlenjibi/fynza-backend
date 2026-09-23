package ecommerce.modules.shipping;

import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.exception.InvalidShipmentTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ShipmentStateMachine {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = new EnumMap<>(ShipmentStatus.class);

    static {
        ALLOWED.put(ShipmentStatus.DRAFT,            EnumSet.of(ShipmentStatus.READY, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.READY,            EnumSet.of(ShipmentStatus.LABEL_CREATED, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.LABEL_CREATED,    EnumSet.of(ShipmentStatus.PICKUP_SCHEDULED, ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.PICKUP_SCHEDULED, EnumSet.of(ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.PICKED_UP,        EnumSet.of(ShipmentStatus.IN_TRANSIT));
        ALLOWED.put(ShipmentStatus.IN_TRANSIT,       EnumSet.of(ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERY_FAILED, ShipmentStatus.LOST, ShipmentStatus.DAMAGED));
        ALLOWED.put(ShipmentStatus.OUT_FOR_DELIVERY, EnumSet.of(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED));
        ALLOWED.put(ShipmentStatus.DELIVERED,        EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.DELIVERY_FAILED,  EnumSet.of(ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.RETURN_TO_SENDER));
        ALLOWED.put(ShipmentStatus.RETURN_TO_SENDER, EnumSet.of(ShipmentStatus.RETURNED));
        ALLOWED.put(ShipmentStatus.RETURNED,         EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.CANCELLED,        EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.LOST,             EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.DAMAGED,          EnumSet.noneOf(ShipmentStatus.class));
    }

    private ShipmentStateMachine() {}

    public static void validate(ShipmentStatus from, ShipmentStatus to) {
        Set<ShipmentStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ShipmentStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidShipmentTransitionException(
                    "Invalid shipment transition: " + from + " -> " + to);
        }
    }

    public static boolean canTransition(ShipmentStatus from, ShipmentStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(ShipmentStatus.class)).contains(to);
    }
}

package ecommerce.modules.shipping;

import ecommerce.modules.shipping.enums.FulfillmentStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class FulfillmentStateMachine {

    private static final Map<FulfillmentStatus, Set<FulfillmentStatus>> ALLOWED =
            new EnumMap<>(FulfillmentStatus.class);

    static {
        ALLOWED.put(FulfillmentStatus.PENDING,        EnumSet.of(FulfillmentStatus.PROCESSING, FulfillmentStatus.CANCELLED));
        ALLOWED.put(FulfillmentStatus.PROCESSING,     EnumSet.of(FulfillmentStatus.PACKED, FulfillmentStatus.CANCELLED));
        ALLOWED.put(FulfillmentStatus.PACKED,         EnumSet.of(FulfillmentStatus.READY_TO_SHIP, FulfillmentStatus.CANCELLED));
        ALLOWED.put(FulfillmentStatus.READY_TO_SHIP,  EnumSet.of(FulfillmentStatus.SHIPPED, FulfillmentStatus.CANCELLED));
        ALLOWED.put(FulfillmentStatus.SHIPPED,        EnumSet.of(FulfillmentStatus.COMPLETED));
        ALLOWED.put(FulfillmentStatus.COMPLETED,      EnumSet.noneOf(FulfillmentStatus.class));
        ALLOWED.put(FulfillmentStatus.CANCELLED,      EnumSet.noneOf(FulfillmentStatus.class));
    }

    private FulfillmentStateMachine() {}

    public static void validate(FulfillmentStatus from, FulfillmentStatus to) {
        Set<FulfillmentStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(FulfillmentStatus.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                    "Invalid fulfillment transition: " + from + " -> " + to +
                    ". Allowed: " + allowed);
        }
    }

    public static boolean canTransition(FulfillmentStatus from, FulfillmentStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(FulfillmentStatus.class)).contains(to);
    }
}

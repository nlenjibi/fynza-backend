package ecommerce.modules.refund;

import ecommerce.modules.refund.enums.ReturnStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ReturnStateMachine {

    private static final Map<ReturnStatus, Set<ReturnStatus>> TRANSITIONS =
            new EnumMap<>(ReturnStatus.class);

    static {
        TRANSITIONS.put(ReturnStatus.REQUESTED,          EnumSet.of(ReturnStatus.UNDER_REVIEW, ReturnStatus.CANCELLED));
        TRANSITIONS.put(ReturnStatus.UNDER_REVIEW,       EnumSet.of(ReturnStatus.APPROVED, ReturnStatus.REJECTED, ReturnStatus.CANCELLED));
        TRANSITIONS.put(ReturnStatus.APPROVED,           EnumSet.of(ReturnStatus.RETURN_SHIPPING, ReturnStatus.CANCELLED));
        TRANSITIONS.put(ReturnStatus.REJECTED,           EnumSet.noneOf(ReturnStatus.class));
        TRANSITIONS.put(ReturnStatus.RETURN_SHIPPING,    EnumSet.of(ReturnStatus.IN_TRANSIT));
        TRANSITIONS.put(ReturnStatus.IN_TRANSIT,         EnumSet.of(ReturnStatus.RECEIVED));
        TRANSITIONS.put(ReturnStatus.RECEIVED,           EnumSet.of(ReturnStatus.INSPECTION));
        TRANSITIONS.put(ReturnStatus.INSPECTION,         EnumSet.of(ReturnStatus.APPROVED_FOR_REFUND, ReturnStatus.PARTIALLY_APPROVED, ReturnStatus.REJECTED));
        TRANSITIONS.put(ReturnStatus.APPROVED_FOR_REFUND,EnumSet.of(ReturnStatus.REFUND_REQUESTED));
        TRANSITIONS.put(ReturnStatus.PARTIALLY_APPROVED, EnumSet.of(ReturnStatus.REFUND_REQUESTED));
        TRANSITIONS.put(ReturnStatus.REFUND_REQUESTED,   EnumSet.of(ReturnStatus.RESOLVED, ReturnStatus.FAILED));
        TRANSITIONS.put(ReturnStatus.RESOLVED,           EnumSet.noneOf(ReturnStatus.class));
        TRANSITIONS.put(ReturnStatus.CANCELLED,          EnumSet.noneOf(ReturnStatus.class));
        TRANSITIONS.put(ReturnStatus.EXPIRED,            EnumSet.noneOf(ReturnStatus.class));
        TRANSITIONS.put(ReturnStatus.FAILED,             EnumSet.of(ReturnStatus.REFUND_REQUESTED));
    }

    private ReturnStateMachine() {}

    public static boolean canTransition(ReturnStatus from, ReturnStatus to) {
        Set<ReturnStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void validate(ReturnStatus from, ReturnStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                    "Invalid return status transition: " + from + " → " + to);
        }
    }
}

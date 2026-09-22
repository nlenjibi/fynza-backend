package ecommerce.modules.payout.enums;

public enum PayoutStatus {
    REQUESTED,
    PENDING_APPROVAL,
    APPROVED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSED,
    ON_HOLD
}

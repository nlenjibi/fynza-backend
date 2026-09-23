package ecommerce.modules.refund.enums;

public enum ReconciliationResult {
    MATCHED,
    REFUND_MISSING,
    SHIPMENT_MISSING,
    INVENTORY_MISSING,
    STATUS_MISMATCH,
    AMOUNT_MISMATCH,
    UNKNOWN
}

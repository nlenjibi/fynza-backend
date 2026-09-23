package ecommerce.modules.refund.enums;

public enum ReturnAuditAction {
    CREATED,
    REVIEW_STARTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    ESCALATED,
    SHIPMENT_INITIATED,
    IN_TRANSIT,
    RECEIVED,
    EVIDENCE_ADDED,
    NOTE_ADDED,
    STATUS_CHANGED
}

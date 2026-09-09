package ecommerce.common.enums;

public enum SellerStatus {
    // Full lifecycle (seller module)
    DRAFT,
    PENDING_VERIFICATION,
    UNDER_REVIEW,
    ACTIVE,
    SUSPENDED,
    REJECTED,
    BLOCKED,
    CLOSED,
    // Legacy — kept for seller_profiles backward compatibility
    PENDING
}

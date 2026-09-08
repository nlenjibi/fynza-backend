package ecommerce.common.enums;

public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    LOCKED,
    DISABLED,
    DELETED,
    /** @deprecated use DISABLED */
    @Deprecated INACTIVE,
    /** @deprecated use SUSPENDED */
    @Deprecated BLOCKED
}

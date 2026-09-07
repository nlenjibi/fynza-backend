package ecommerce.modules.audit.constant;

public enum AuditStatus {
    SUCCESS,
    FAILED;

    public static AuditStatus from(String value) {
        if (value == null) return null;
        try {
            return AuditStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

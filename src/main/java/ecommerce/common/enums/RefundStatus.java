package ecommerce.common.enums;

public enum RefundStatus {
    PENDING("Pending", "Refund request is pending review"),
    APPROVED("Approved", "Refund has been approved"),
    REJECTED("Rejected", "Refund has been rejected"),
    COMPLETED("Completed", "Refund has been processed and completed"),
    CANCELLED("Cancelled", "Refund request was cancelled");

    private final String displayName;
    private final String description;

    RefundStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

package ecommerce.common.enums;

public enum RefundReason {
    DAMAGED_PRODUCT("Damaged Product", "Product arrived damaged or defective"),
    WRONG_ITEM("Wrong Item", "Received wrong item"),
    NOT_AS_DESCRIBED("Not As Described", "Product not as described"),
    QUALITY_ISSUE("Quality Issue", "Quality not meeting expectations"),
    CHANGED_MIND("Changed Mind", "Customer changed their mind"),
    DUPLICATE_ORDER("Duplicate Order", "Accidental duplicate order"),
    MISSING_DELIVERY("Missing Delivery", "Order never received"),
    OTHER("Other", "Other reason not listed");

    private final String displayName;
    private final String description;

    RefundReason(String displayName, String description) {
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

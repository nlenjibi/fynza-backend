package ecommerce.common.enums;

/**
 * OrderStatus enum for unified order lifecycle management.
 * Includes display names and descriptions for better UX and tracking.
 */
public enum OrderStatus {
    PENDING("Pending", "Order is pending confirmation"),
    CONFIRMED("Confirmed", "Order has been confirmed"),
    PROCESSING("Processing", "Order is being processed"),
    VALIDATION_FAILED("Validation Failed", "Order validation failed"),
    VALIDATED("Validated", "Order validated successfully"),
    SHIPPED("Shipped", "Order has been shipped"),
    IN_TRANSIT("In Transit", "Order is in transit"),
    OUT_FOR_DELIVERY("Out for Delivery", "Order is out for delivery"),
    DELIVERED("Delivered", "Order has been delivered"),
    CANCELLED("Cancelled", "Order has been cancelled"),
    REFUND_REQUESTED("Refund Requested", "Refund has been requested"),
    REFUNDED("Refunded", "Order has been refunded"),
    FAILED("Failed", "Order failed"),
    RETURNED("Returned", "Order has been returned"),
    PAYMENT_FAILED("Payment Failed", "Payment for the order failed");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
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

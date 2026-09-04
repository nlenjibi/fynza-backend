package ecommerce.common.enums;

public enum ReportType {
    SALES("Sales Report", "Detailed breakdown of all sales transactions", "GREEN"),
    REVENUE("Revenue Report", "Revenue breakdown by category, seller, period", "BLUE"),
    ORDER("Order Report", "Complete order details and status tracking", "PURPLE"),
    SELLER_PERFORMANCE("Seller Performance", "Seller metrics, sales, ratings, activity", "ORANGE"),
    PRODUCT_PERFORMANCE("Product Performance", "Top products, inventory, sales data", "CYAN"),
    REFUND("Refund Report", "Refund requests, reasons, resolution times", "RED"),
    CUSTOMER_ANALYTICS("Customer Analytics", "Customer acquisition, retention, behavior", "INDIGO"),
    CATEGORY_PERFORMANCE("Category Performance", "Sales and revenue by category", "PINK");

    private final String displayName;
    private final String description;
    private final String color;

    ReportType(String displayName, String description, String color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }
}

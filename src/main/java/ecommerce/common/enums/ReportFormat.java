package ecommerce.common.enums;

public enum ReportFormat {
    PDF("PDF", "application/pdf"),
    CSV("CSV", "text/csv"),
    EXCEL("Excel", "application/vnd.ms-excel");

    private final String displayName;
    private final String mimeType;

    ReportFormat(String displayName, String mimeType) {
        this.displayName = displayName;
        this.mimeType = mimeType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }
}

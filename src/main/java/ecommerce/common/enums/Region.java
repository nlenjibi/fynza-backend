package ecommerce.common.enums;

public enum Region {
    GREATER_ACCRA("Greater Accra"),
    ASHANTI("Ashanti"),
    WESTERN("Western"),
    CENTRAL("Central"),
    EASTERN("Eastern"),
    VOLTA("Volta"),
    NORTHERN("Northern"),
    UPPER_EAST("Upper East"),
    UPPER_WEST("Upper West"),
    BONO("Bono"),
    BONO_EAST("Bono East"),
    AHAFO("Ahafo"),
    OTI("Oti"),
    WESTERN_NORTH("Western North"),
    SAVANNAH("Savannah"),
    NORTH_EAST("North East");

    private final String displayName;

    Region(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package ecommerce.modules.shipping.dto.request;

import lombok.Data;

@Data
public class ShipmentAddressRequest {
    private String recipientName;
    private String recipientPhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}

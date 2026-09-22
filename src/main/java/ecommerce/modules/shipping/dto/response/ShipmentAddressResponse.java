package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShipmentAddress;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShipmentAddressResponse {
    String recipientName;
    String recipientPhone;
    String addressLine1;
    String addressLine2;
    String city;
    String state;
    String country;
    String postalCode;

    public static ShipmentAddressResponse from(ShipmentAddress address) {
        if (address == null) return null;
        return ShipmentAddressResponse.builder()
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .build();
    }
}

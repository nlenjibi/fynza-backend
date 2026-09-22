package ecommerce.modules.shipping.provider;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ShipmentProviderRequest {
    String recipientName;
    String recipientPhone;
    String addressLine1;
    String city;
    String country;
    BigDecimal weightKg;
    String serviceCode;
    BigDecimal declaredValue;
}

package ecommerce.modules.shipping.provider;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class ShipmentProviderResult {
    String trackingNumber;
    String labelUrl;
    String carrierLabelId;
    BigDecimal cost;
    LocalDate estimatedDeliveryDate;
}

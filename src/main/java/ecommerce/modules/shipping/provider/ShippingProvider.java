package ecommerce.modules.shipping.provider;

import ecommerce.modules.shipping.provider.dto.LabelResult;
import ecommerce.modules.shipping.provider.dto.TrackingResult;

import java.util.UUID;

public interface ShippingProvider {

    ShippingProviderType providerType();

    ShipmentProviderResult createShipment(ShipmentProviderRequest request);

    boolean cancelShipment(String trackingNumber);

    LabelResult generateLabel(UUID shipmentPublicId, ShipmentProviderRequest request);

    TrackingResult getTracking(String trackingNumber);
}

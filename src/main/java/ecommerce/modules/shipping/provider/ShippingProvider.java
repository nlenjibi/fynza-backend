package ecommerce.modules.shipping.provider;

public interface ShippingProvider {

    ShippingProviderType providerType();

    ShipmentProviderResult createShipment(ShipmentProviderRequest request);

    boolean cancelShipment(String trackingNumber);
}

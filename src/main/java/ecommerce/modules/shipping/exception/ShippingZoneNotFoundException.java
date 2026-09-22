package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.ResourceNotFoundException;

public class ShippingZoneNotFoundException extends ResourceNotFoundException {
    public ShippingZoneNotFoundException(String message) {
        super(message);
    }
}

package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.ResourceNotFoundException;

public class ShippingRateNotFoundException extends ResourceNotFoundException {
    public ShippingRateNotFoundException(String message) {
        super(message);
    }
}

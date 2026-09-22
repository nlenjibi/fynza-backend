package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.ResourceNotFoundException;

public class ShippingMethodNotFoundException extends ResourceNotFoundException {
    public ShippingMethodNotFoundException(String message) {
        super(message);
    }
}

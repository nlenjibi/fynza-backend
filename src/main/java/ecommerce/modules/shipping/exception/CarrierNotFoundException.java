package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.ResourceNotFoundException;

public class CarrierNotFoundException extends ResourceNotFoundException {
    public CarrierNotFoundException(String message) {
        super(message);
    }
}

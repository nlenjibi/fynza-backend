package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.ResourceNotFoundException;

public class ShipmentNotFoundException extends ResourceNotFoundException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }
}

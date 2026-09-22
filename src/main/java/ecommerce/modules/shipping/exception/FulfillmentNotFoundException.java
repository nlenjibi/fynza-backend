package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.ResourceNotFoundException;

public class FulfillmentNotFoundException extends ResourceNotFoundException {
    public FulfillmentNotFoundException(String message) {
        super(message);
    }
}

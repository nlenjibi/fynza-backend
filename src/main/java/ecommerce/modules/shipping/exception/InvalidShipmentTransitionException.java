package ecommerce.modules.shipping.exception;

import ecommerce.common.exception.BadRequestException;

public class InvalidShipmentTransitionException extends BadRequestException {
    public InvalidShipmentTransitionException(String message) {
        super(message);
    }
}

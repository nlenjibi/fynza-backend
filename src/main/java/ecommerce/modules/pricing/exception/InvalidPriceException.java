package ecommerce.modules.pricing.exception;

import ecommerce.common.exception.BadRequestException;

public class InvalidPriceException extends BadRequestException {

    public InvalidPriceException(String message) {
        super(message);
    }
}

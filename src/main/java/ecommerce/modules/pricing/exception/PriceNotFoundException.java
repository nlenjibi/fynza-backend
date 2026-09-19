package ecommerce.modules.pricing.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class PriceNotFoundException extends ResourceNotFoundException {

    public PriceNotFoundException(UUID publicId) {
        super("Price not found: " + publicId, "PRICE_NOT_FOUND");
    }
}

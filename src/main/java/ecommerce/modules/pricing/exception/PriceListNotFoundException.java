package ecommerce.modules.pricing.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class PriceListNotFoundException extends ResourceNotFoundException {

    public PriceListNotFoundException(UUID publicId) {
        super("Price list not found: " + publicId, "PRICE_LIST_NOT_FOUND");
    }

    public PriceListNotFoundException(String detail) {
        super("Price list not found: " + detail, "PRICE_LIST_NOT_FOUND");
    }
}

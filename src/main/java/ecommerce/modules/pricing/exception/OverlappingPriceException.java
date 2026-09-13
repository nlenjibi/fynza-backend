package ecommerce.modules.pricing.exception;

import ecommerce.common.exception.ConflictException;

public class OverlappingPriceException extends ConflictException {

    public OverlappingPriceException() {
        super("An ACTIVE price already exists for this product/variant and currency combination in this price list.");
    }
}

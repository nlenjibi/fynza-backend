package ecommerce.modules.pricing.exception;

import ecommerce.common.exception.ForbiddenException;

import java.util.UUID;

public class PriceOwnershipException extends ForbiddenException {

    public PriceOwnershipException(UUID pricePublicId) {
        super("Access denied: you do not own price " + pricePublicId);
    }
}

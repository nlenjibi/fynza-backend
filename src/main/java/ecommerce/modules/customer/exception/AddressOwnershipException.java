package ecommerce.modules.customer.exception;

import ecommerce.common.exception.ForbiddenException;

public class AddressOwnershipException extends ForbiddenException {

    public AddressOwnershipException() {
        super("Address does not belong to this customer", "ADDRESS_OWNERSHIP_VIOLATION");
    }
}

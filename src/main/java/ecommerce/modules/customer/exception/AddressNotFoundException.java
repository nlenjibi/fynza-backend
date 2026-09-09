package ecommerce.modules.customer.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class AddressNotFoundException extends ResourceNotFoundException {

    public AddressNotFoundException(UUID publicId) {
        super("Address not found: " + publicId, "ADDRESS_NOT_FOUND");
    }
}

package ecommerce.modules.customer.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(UUID publicId) {
        super("Customer not found: " + publicId, "CUSTOMER_NOT_FOUND");
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
}

package ecommerce.modules.customer.exception;

import ecommerce.common.exception.DuplicateResourceException;

import java.util.UUID;

public class CustomerAlreadyExistsException extends DuplicateResourceException {

    public CustomerAlreadyExistsException(UUID userId) {
        super("Customer already exists for user: " + userId, "CUSTOMER_ALREADY_EXISTS");
    }
}

package ecommerce.modules.store.exception;

import ecommerce.common.exception.DuplicateResourceException;

import java.util.UUID;

public class StoreAlreadyExistsException extends DuplicateResourceException {

    public StoreAlreadyExistsException(UUID sellerId) {
        super("Store already exists for seller: " + sellerId, "STORE_ALREADY_EXISTS");
    }

    public StoreAlreadyExistsException(String message) {
        super(message, "STORE_ALREADY_EXISTS");
    }
}

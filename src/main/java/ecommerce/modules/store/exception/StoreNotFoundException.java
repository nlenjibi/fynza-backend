package ecommerce.modules.store.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class StoreNotFoundException extends ResourceNotFoundException {

    public StoreNotFoundException(UUID publicId) {
        super("Store not found: " + publicId, "STORE_NOT_FOUND");
    }

    public StoreNotFoundException(String message) {
        super(message);
    }
}

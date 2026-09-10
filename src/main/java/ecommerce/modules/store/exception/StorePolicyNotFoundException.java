package ecommerce.modules.store.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class StorePolicyNotFoundException extends ResourceNotFoundException {

    public StorePolicyNotFoundException(UUID publicId) {
        super("Store policy not found: " + publicId, "STORE_POLICY_NOT_FOUND");
    }
}

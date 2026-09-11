package ecommerce.modules.store.exception;

import ecommerce.common.exception.DuplicateResourceException;

public class StoreSlugAlreadyTakenException extends DuplicateResourceException {

    public StoreSlugAlreadyTakenException(String slug) {
        super("Store slug already taken: " + slug, "STORE_SLUG_TAKEN");
    }
}

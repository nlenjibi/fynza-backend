package ecommerce.modules.seller.exception;

import java.util.UUID;

public class SellerAlreadyExistsException extends RuntimeException {

    public SellerAlreadyExistsException(UUID userId) {
        super("A seller account already exists for user: " + userId);
    }
}

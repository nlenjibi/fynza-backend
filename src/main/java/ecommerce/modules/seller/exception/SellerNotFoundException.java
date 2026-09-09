package ecommerce.modules.seller.exception;

import java.util.UUID;

public class SellerNotFoundException extends RuntimeException {

    public SellerNotFoundException(UUID publicId) {
        super("Seller not found: " + publicId);
    }

    public SellerNotFoundException(String message) {
        super(message);
    }
}

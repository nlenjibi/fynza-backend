package ecommerce.modules.product.exception;

import java.util.UUID;

public class ProductOwnershipException extends RuntimeException {

    public ProductOwnershipException(UUID productId) {
        super("Access denied: you do not own product " + productId);
    }
}

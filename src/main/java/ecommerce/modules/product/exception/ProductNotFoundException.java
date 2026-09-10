package ecommerce.modules.product.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("Product not found: " + id);
    }

    public ProductNotFoundException(String identifier) {
        super("Product not found: " + identifier);
    }
}

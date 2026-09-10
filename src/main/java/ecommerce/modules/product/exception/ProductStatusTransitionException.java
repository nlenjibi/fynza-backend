package ecommerce.modules.product.exception;

import ecommerce.common.enums.ProductStatus;

public class ProductStatusTransitionException extends RuntimeException {

    public ProductStatusTransitionException(ProductStatus from, ProductStatus to) {
        super("Invalid product status transition: " + from + " → " + to);
    }
}

package ecommerce.common.exception;


public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int available, int requested) {
        super(String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                productName, available, requested));
    }

}

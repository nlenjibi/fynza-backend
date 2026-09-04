package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends FynzaException {

    public InsufficientStockException(String productName, int available, int requested) {
        super(String.format("Insufficient stock for '%s'. Available: %d, Requested: %d",
                productName, available, requested),
              HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_STOCK");
    }
}

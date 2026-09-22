package ecommerce.modules.payout.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends FynzaException {

    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE");
    }
}

package ecommerce.modules.payout.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class PayoutAccountException extends FynzaException {

    public PayoutAccountException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "PAYOUT_ACCOUNT_ERROR");
    }
}

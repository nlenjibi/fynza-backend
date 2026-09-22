package ecommerce.modules.payout.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class PayoutEligibilityException extends FynzaException {

    public PayoutEligibilityException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "PAYOUT_ELIGIBILITY_FAILED");
    }
}
